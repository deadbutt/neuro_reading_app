package com.example.neuro.repository

import com.example.neuro.api.ApiService
import com.example.neuro.api.model.ArticleMeta
import com.example.neuro.api.model.BookshelfItemResponse
import com.example.neuro.api.model.ChapterContentResponse
import com.example.neuro.api.model.CommentResponse
import com.example.neuro.api.model.PaginatedResponse
import com.example.neuro.api.model.PostCommentRequest
import com.example.neuro.util.ApiHelper
import com.example.neuro.util.ApiResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun getArticleDetail(articleId: String): ApiResult<ArticleMeta> {
        return ApiHelper.safeApiCallWithMessage {
            apiService.getArticleDetail(articleId)
        }
    }

    suspend fun getBookshelf(
        page: Int = 1,
        pageSize: Int = 20
    ): ApiResult<PaginatedResponse<BookshelfItemResponse>> {
        return ApiHelper.safeApiCallWithMessage {
            apiService.getBookshelf(page, pageSize)
        }
    }

    suspend fun getChapterContent(
        articleId: String,
        chapterIndex: Int
    ): ApiResult<ChapterContentResponse> {
        return ApiHelper.safeApiCallWithMessage {
            apiService.getChapterContent(articleId, chapterIndex)
        }
    }

    suspend fun getArticleComments(
        articleId: String,
        page: Int = 1,
        pageSize: Int = 20,
        sort: String = "hot"
    ): ApiResult<PaginatedResponse<CommentResponse>> {
        return ApiHelper.safeApiCallWithMessage {
            apiService.getArticleComments(articleId, page, pageSize, sort)
        }
    }

    suspend fun addToBookshelf(articleId: String): ApiResult<Unit> {
        return ApiHelper.safeApiCallWithMessage {
            apiService.addToBookshelf(articleId)
        }
    }

    suspend fun removeFromBookshelf(articleId: String): ApiResult<Unit> {
        return ApiHelper.safeApiCallWithMessage {
            apiService.removeFromBookshelf(articleId)
        }
    }

    suspend fun postComment(articleId: String, request: PostCommentRequest): ApiResult<CommentResponse> {
        return ApiHelper.safeApiCallWithMessage {
            apiService.postComment(articleId, request)
        }
    }
}
