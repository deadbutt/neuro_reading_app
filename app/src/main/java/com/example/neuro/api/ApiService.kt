package com.example.neuro.api

import com.example.neuro.api.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ==================== 认证相关 ====================

    @POST("api/v1/auth/send-code")
    suspend fun sendVerificationCode(@Body request: SendCodeRequest): Response<BaseResponse<Unit>>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<BaseResponse<LoginResponse>>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<BaseResponse<LoginResponse>>

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<BaseResponse<Unit>>

    @POST("api/v1/auth/refresh-token")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<BaseResponse<LoginResponse>>

    // ==================== 用户相关 ====================

    @GET("api/v1/user/profile")
    suspend fun getUserProfile(): Response<BaseResponse<UserProfileResponse>>

    @PUT("api/v1/user/profile")
    suspend fun updateUserProfile(@Body request: UpdateProfileRequest): Response<BaseResponse<UserProfileResponse>>

    @POST("api/v1/user/follow/{authorId}")
    suspend fun followAuthor(@Path("authorId") authorId: String): Response<BaseResponse<Unit>>

    @DELETE("api/v1/user/follow/{authorId}")
    suspend fun unfollowAuthor(@Path("authorId") authorId: String): Response<BaseResponse<Unit>>

    @GET("api/v1/user/following")
    suspend fun getFollowingList(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<BaseResponse<PaginatedResponse<AuthorResponse>>>

    // ==================== 书架相关 ====================

    @GET("api/v1/bookshelf")
    suspend fun getBookshelf(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<BaseResponse<PaginatedResponse<BookshelfItemResponse>>>

    @POST("api/v1/bookshelf/{articleId}")
    suspend fun addToBookshelf(@Path("articleId") articleId: String): Response<BaseResponse<Unit>>

    @DELETE("api/v1/bookshelf/{articleId}")
    suspend fun removeFromBookshelf(@Path("articleId") articleId: String): Response<BaseResponse<Unit>>

    @PUT("api/v1/bookshelf/{articleId}/progress")
    suspend fun updateReadingProgress(
        @Path("articleId") articleId: String,
        @Body request: UpdateProgressRequest
    ): Response<BaseResponse<Unit>>

    // ==================== 文章相关 ====================

    @GET("api/v1/articles")
    suspend fun getArticles(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Response<BaseResponse<PaginatedResponse<ArticleIndex>>>

    @GET("api/v1/articles/search")
    suspend fun searchArticles(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<BaseResponse<List<ArticleIndex>>>

    @GET("api/v1/articles/{articleId}")
    suspend fun getArticleDetail(@Path("articleId") articleId: String): Response<BaseResponse<ArticleMeta>>

    @GET("api/v1/articles/{articleId}/chapters/{chapterIndex}")
    suspend fun getChapterContent(
        @Path("articleId") articleId: String,
        @Path("chapterIndex") chapterIndex: Int
    ): Response<BaseResponse<ChapterContentResponse>>

    // ==================== 评论相关 ====================

    @GET("api/v1/articles/{articleId}/comments")
    suspend fun getArticleComments(
        @Path("articleId") articleId: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("sort") sort: String = "hot"
    ): Response<BaseResponse<PaginatedResponse<CommentResponse>>>

    @POST("api/v1/articles/{articleId}/comments")
    suspend fun postComment(
        @Path("articleId") articleId: String,
        @Body request: PostCommentRequest
    ): Response<BaseResponse<CommentResponse>>

    @POST("api/v1/comments/{commentId}/like")
    suspend fun likeComment(@Path("commentId") commentId: String): Response<BaseResponse<Unit>>

    @DELETE("api/v1/comments/{commentId}/like")
    suspend fun unlikeComment(@Path("commentId") commentId: String): Response<BaseResponse<Unit>>

    // ==================== 作者相关 ====================

    @GET("api/v1/authors/{authorId}")
    suspend fun getAuthorProfile(@Path("authorId") authorId: String): Response<BaseResponse<AuthorProfileResponse>>

    @GET("api/v1/authors/{authorId}/works")
    suspend fun getAuthorWorks(
        @Path("authorId") authorId: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<BaseResponse<PaginatedResponse<ArticleIndex>>>

    @GET("api/v1/authors/{authorId}/activities")
    suspend fun getAuthorActivities(
        @Path("authorId") authorId: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<BaseResponse<PaginatedResponse<AuthorActivityResponse>>>

    // ==================== 动态/关注流相关 ====================

    @GET("api/v1/feed")
    suspend fun getFeedList(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<BaseResponse<PaginatedResponse<FeedActivityResponse>>>

    @POST("api/v1/feed/{feedId}/like")
    suspend fun likeFeed(@Path("feedId") feedId: String): Response<BaseResponse<Unit>>

    @DELETE("api/v1/feed/{feedId}/like")
    suspend fun unlikeFeed(@Path("feedId") feedId: String): Response<BaseResponse<Unit>>

    // ==================== 头像上传 ====================

    @Multipart
    @POST("api/v1/upload/avatar")
    suspend fun uploadAvatar(
        @Part file: MultipartBody.Part
    ): Response<BaseResponse<UploadAvatarResponse>>
}
