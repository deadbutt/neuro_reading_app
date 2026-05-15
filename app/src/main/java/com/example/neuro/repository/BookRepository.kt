package com.example.neuro.repository

import com.example.neuro.Constants
import com.example.neuro.api.ApiService
import com.example.neuro.api.model.ArticleIndex
import com.example.neuro.util.ApiHelper
import com.example.neuro.util.ApiResult
import com.example.neuro.util.UrlUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository @Inject constructor(
    private val apiService: ApiService
) {

    private val cache = mutableMapOf<String, CacheEntry<List<ArticleIndex>>>()
    private val cacheDuration = 5 * 60 * 1000L
    private val maxCacheSize = 50

    data class CacheEntry<T>(
        val data: T,
        val timestamp: Long
    )

    suspend fun getArticles(
        type: Int,
        page: Int,
        pageSize: Int = 10,
        forceRefresh: Boolean = false
    ): ApiResult<List<ArticleIndex>> {
        val cacheKey = "articles_${type}_$page"

        if (!forceRefresh) {
            cache[cacheKey]?.let { entry ->
                if (System.currentTimeMillis() - entry.timestamp < cacheDuration) {
                    return ApiResult.Success(entry.data)
                }
            }
        }

        return when (val result = ApiHelper.safeApiCallWithMessage {
            apiService.getArticles(page = page, pageSize = pageSize)
        }) {
            is ApiResult.Success -> {
                val articles = result.data.safeList()
                putCache(cacheKey, articles)
                ApiResult.Success(articles)
            }
            is ApiResult.Error -> result
            ApiResult.Loading -> ApiResult.Loading
        }
    }

    private fun putCache(key: String, data: List<ArticleIndex>) {
        if (cache.size >= maxCacheSize) {
            cache.entries.minByOrNull { it.value.timestamp }?.let {
                cache.remove(it.key)
            }
        }
        cache[key] = CacheEntry(data, System.currentTimeMillis())
    }

    fun clearCache() {
        cache.clear()
    }

    suspend fun searchArticles(
        keyword: String,
        page: Int = 1,
        pageSize: Int = 20
    ): ApiResult<List<ArticleIndex>> {
        return ApiHelper.safeApiCallWithMessage {
            apiService.searchArticles(keyword = keyword, page = page, pageSize = pageSize)
        }
    }

    fun normalizeUrl(url: String?): String {
        return UrlUtils.normalize(url)
    }
}
