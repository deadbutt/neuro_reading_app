package com.example.neuro.api.model

// 后端返回的作品数据格式（作者主页作品列表）
data class BookResponse(
    val bookId: String,
    val title: String,
    val author: AuthorResponse,
    val cover: String?,
    val description: String,
    val hotText: String,
    val wordCount: Long,
    val chapterCount: Int,
    val status: String,
    val tags: List<String>?,
    val lastUpdateTime: String,
    val isVip: Boolean
)
