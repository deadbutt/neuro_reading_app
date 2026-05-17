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

    @GET("api/v1/user/reading-history")
    suspend fun getReadingHistory(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<BaseResponse<PaginatedResponse<ReadingHistoryResponse>>>

    @DELETE("api/v1/user/reading-history/{historyId}")
    suspend fun deleteReadingHistory(@Path("historyId") historyId: String): Response<BaseResponse<Unit>>

    @DELETE("api/v1/user/reading-history")
    suspend fun clearReadingHistory(): Response<BaseResponse<Unit>>

    // ==================== 书架相关 ====================

    @GET("api/v1/bookshelf")
    suspend fun getBookshelf(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("category") category: String = "all"
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

    @POST("api/v1/bookshelf/{articleId}/favorite")
    suspend fun toggleFavorite(@Path("articleId") articleId: String): Response<BaseResponse<FavoriteStatusResponse>>

    @GET("api/v1/bookshelf/{articleId}/favorite")
    suspend fun getFavoriteStatus(@Path("articleId") articleId: String): Response<BaseResponse<FavoriteStatusResponse>>

    // ==================== 消息通知相关 ====================

    @GET("api/v1/notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<BaseResponse<PaginatedResponse<NotificationResponse>>>

    @GET("api/v1/notifications/count")
    suspend fun getNotificationCount(): Response<BaseResponse<NotificationCountResponse>>

    @POST("api/v1/notifications/{notificationId}/read")
    suspend fun markNotificationAsRead(@Path("notificationId") notificationId: String): Response<BaseResponse<Unit>>

    @POST("api/v1/notifications/read-all")
    suspend fun markAllNotificationsAsRead(): Response<BaseResponse<Unit>>

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
    ): Response<BaseResponse<PaginatedResponse<BookResponse>>>

    @GET("api/v1/authors/{authorId}/activities")
    suspend fun getAuthorActivities(
        @Path("authorId") authorId: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<BaseResponse<PaginatedResponse<AuthorActivityResponse>>>

    @GET("api/v1/authors/{authorId}/follow-status")
    suspend fun getAuthorFollowStatus(
        @Path("authorId") authorId: String
    ): Response<BaseResponse<FollowStatusResponse>>

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

    // ==================== 作品上传 ====================

    @Multipart
    @POST("api/v1/creator/works/upload/txt")
    suspend fun uploadTxtWork(
        @Part file: MultipartBody.Part,
        @Part("title") title: okhttp3.RequestBody?,
        @Part("summary") summary: okhttp3.RequestBody?,
        @Part("tags") tags: okhttp3.RequestBody?,
        @Part("cover") cover: okhttp3.RequestBody?
    ): Response<BaseResponse<UploadWorkResponse>>

    @Multipart
    @POST("api/v1/creator/works/upload/docx")
    suspend fun uploadDocx(
        @Part file: MultipartBody.Part,
        @Part("title") title: okhttp3.RequestBody?,
        @Part("cover") cover: okhttp3.RequestBody?
    ): Response<BaseResponse<UploadDocxResponse>>

    // ==================== 创作中心 ====================

    @GET("api/v1/creator/works")
    suspend fun getMyWorks(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("status") status: String = "all"
    ): Response<BaseResponse<MyWorksResponse>>

    @POST("api/v1/creator/works")
    suspend fun createWork(
        @Body request: CreateWorkRequest
    ): Response<BaseResponse<CreateWorkResponse>>

    @GET("api/v1/creator/works/{workId}")
    suspend fun getWorkDetail(
        @Path("workId") workId: String
    ): Response<BaseResponse<WorkDetailResponse>>

    @PUT("api/v1/creator/works/{workId}")
    suspend fun updateWork(
        @Path("workId") workId: String,
        @Body request: UpdateWorkRequest
    ): Response<BaseResponse<Unit>>

    @DELETE("api/v1/creator/works/{workId}")
    suspend fun deleteWork(
        @Path("workId") workId: String
    ): Response<BaseResponse<Unit>>

    @POST("api/v1/creator/works/{workId}/publish")
    suspend fun publishWork(
        @Path("workId") workId: String
    ): Response<BaseResponse<PublishWorkResponse>>

    @POST("api/v1/creator/works/{workId}/chapters")
    suspend fun createChapter(
        @Path("workId") workId: String,
        @Body request: CreateChapterRequest
    ): Response<BaseResponse<ChapterResponse>>

    @GET("api/v1/creator/works/{workId}/chapters/{chapterIndex}")
    suspend fun getChapterForEdit(
        @Path("workId") workId: String,
        @Path("chapterIndex") chapterIndex: Int
    ): Response<BaseResponse<ChapterDetailResponse>>

    @PUT("api/v1/creator/works/{workId}/chapters/{chapterIndex}")
    suspend fun updateChapter(
        @Path("workId") workId: String,
        @Path("chapterIndex") chapterIndex: Int,
        @Body request: UpdateChapterRequest
    ): Response<BaseResponse<Unit>>

    @DELETE("api/v1/creator/works/{workId}/chapters/{chapterIndex}")
    suspend fun deleteChapter(
        @Path("workId") workId: String,
        @Path("chapterIndex") chapterIndex: Int
    ): Response<BaseResponse<Unit>>

    @Multipart
    @POST("api/v1/creator/works/upload/cover")
    suspend fun uploadCover(
        @Part file: MultipartBody.Part
    ): Response<BaseResponse<UploadCoverResponse>>
}
