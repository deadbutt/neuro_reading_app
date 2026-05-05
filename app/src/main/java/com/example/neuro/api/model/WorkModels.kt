package com.example.neuro.api.model

data class CreateWorkRequest(
    val title: String,
    val summary: String? = null,
    val tags: List<String>? = null,
    val cover: String? = null
)

data class CreateWorkResponse(
    val workId: String,
    val title: String,
    val status: String
)

data class CreateChapterRequest(
    val title: String,
    val content: String,
    val status: String = "draft"
)

data class UpdateChapterRequest(
    val title: String? = null,
    val content: String? = null,
    val status: String? = null
)

data class ChapterResponse(
    val chapterId: String,
    val title: String,
    val wordCount: Int,
    val status: String
)

data class ChapterDetailResponse(
    val chapterId: String,
    val workId: String,
    val title: String,
    val content: String,
    val wordCount: Int,
    val status: String,
    val createTime: String,
    val updateTime: String
)

data class MyWorksResponse(
    val list: List<WorkListItem>,
    val total: Int,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean
)

data class WorkListItem(
    val workId: String,
    val title: String,
    val summary: String,
    val cover: String,
    val status: String,
    val chapterCount: Int,
    val wordCount: Int,
    val createTime: String,
    val updateTime: String
)
