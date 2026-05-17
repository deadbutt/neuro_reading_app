package com.example.neuro.api.model

// 作者主页响应
data class AuthorProfileResponse(
    val authorId: String,
    val name: String,
    val avatar: String,
    val description: String,
    val worksCount: Int,
    val followersCount: Int,
    val totalWords: Long,
    val isFollowing: Boolean
)

// 作者动态响应
data class AuthorActivityResponse(
    val activityId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String,
    val type: String,         // "publish"=发布新章节, "announcement"=公告
    val content: String,      // 动态内容
    val bookId: String?,
    val bookTitle: String?,
    val chapterTitle: String?,
    val chapterPreview: String?,
    val readHeat: String?,
    val createTime: String,
    val likeCount: Int,
    val commentCount: Int
)

// 动态流响应（关注流）
data class FeedActivityResponse(
    val feedId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String,
    val publishTime: String,
    val activityContent: String,
    val bookId: String?,
    val bookCover: String?,
    val chapterPreview: String?,
    val readHeat: String?,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean
)

// 关注状态响应
data class FollowStatusResponse(
    val isFollowing: Boolean
)
