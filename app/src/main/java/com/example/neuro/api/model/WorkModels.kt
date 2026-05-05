package com.example.neuro.api.model

data class CreateWorkRequest(
    val title: String,
    val summary: String? = null,
    val tags: List<String>? = null,
    val cover: String? = null
)

data class CreateWorkResponse(
    val articleId: String,
    val creatorId: String,
    val title: String,
    val status: String
)

data class UpdateWorkRequest(
    val title: String? = null,
    val summary: String? = null,
    val tags: List<String>? = null,
    val cover: String? = null
)

data class WorkDetailResponse(
    val articleId: String,
    val creatorId: String,
    val title: String,
    val summary: String,
    val tags: List<String>,
    val cover: String?,
    val status: String,
    val chapters: List<ChapterInfo>,
    val wordCount: Int,
    val publishTime: String?,
    val lastUpdateTime: String
)

data class ChapterInfo(
    val index: Int,
    val chapterId: String,
    val title: String,
    val wordCount: Int
)

data class PublishWorkResponse(
    val articleId: String,
    val creatorId: String,
    val status: String
)

data class CreateChapterRequest(
    val title: String,
    val content: String
)

data class UpdateChapterRequest(
    val title: String? = null,
    val content: String? = null
)

data class ChapterResponse(
    val chapterId: String,
    val index: Int,
    val title: String,
    val wordCount: Int
)

data class ChapterDetailResponse(
    val chapterId: String,
    val index: Int,
    val title: String,
    val content: String,
    val wordCount: Int
)

data class MyWorksResponse(
    val list: List<WorkListItem>,
    val total: Int,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean
)

data class WorkListItem(
    val articleId: String,
    val creatorId: String,
    val title: String,
    val summary: String,
    val cover: String?,
    val status: String?,
    val chapterCount: Int,
    val wordCount: Int,
    val lastUpdateTime: String
)

data class UploadDocxResponse(
    val articleId: String,
    val title: String,
    val chapterCount: Int,
    val wordCount: Int
)

data class UploadCoverResponse(
    val url: String
)
