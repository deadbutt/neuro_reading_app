package com.example.neuro.api.model

// 用户资料响应
data class UserProfileResponse(
    val userId: String,
    val account: String,
    val nickname: String,
    val avatar: String,
    val bio: String,
    val gender: Int,          // 0=保密, 1=男, 2=女
    val followingCount: Int,
    val bookshelfCount: Int,
    val readDuration: Long    // 阅读时长（分钟）
)

// 更新用户资料请求
data class UpdateProfileRequest(
    val nickname: String? = null,
    val avatar: String? = null,
    val bio: String? = null,
    val gender: Int? = null
)

// 书架项响应
data class BookshelfItemResponse(
    val articleId: String,
    val title: String,
    val author: String,       // 作者名（字符串）
    val cover: String,
    val lastReadChapter: String,
    val lastReadTime: String,
    val progress: Int,        // 阅读进度 0-100
    val chapterIndex: Int = 0, // 当前章节索引
    val isFinished: Boolean
)

// 更新阅读进度请求
data class UpdateProgressRequest(
    val chapterIndex: Int,    // 当前章节索引（从0开始）
    val progress: Int,        // 0-100
    val position: Int         // 当前阅读位置（字符偏移）
)

// 阅读历史响应
data class ReadingHistoryResponse(
    val historyId: String,
    val articleId: String,
    val title: String,
    val author: String,
    val cover: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val progress: Int,        // 阅读进度 0-100
    val position: Int,        // 当前阅读位置（字符偏移）
    val readTime: Int,        // 本次阅读时长（分钟）
    val lastReadTime: String
)
