package com.example.neuro.repository

import com.example.neuro.Constants
import com.example.neuro.api.RetrofitClient
import com.example.neuro.api.model.ArticleIndex
import com.example.neuro.util.ApiHelper
import com.example.neuro.util.ApiResult
import com.example.neuro.util.UrlUtils

class BookRepository {
    
    private val apiService = RetrofitClient.apiService
    
    private val cache = mutableMapOf<String, CacheEntry<List<ArticleIndex>>>()
    private val cacheDuration = 5 * 60 * 1000L
    
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
                val articles = result.data.list ?: emptyList()
                cache[cacheKey] = CacheEntry(articles, System.currentTimeMillis())
                ApiResult.Success(articles)
            }
            is ApiResult.Error -> result
            ApiResult.Loading -> ApiResult.Loading
        }
    }
    
    fun clearCache() {
        cache.clear()
    }
    
    fun normalizeUrl(url: String?): String {
        return UrlUtils.normalize(url)
    }
}
