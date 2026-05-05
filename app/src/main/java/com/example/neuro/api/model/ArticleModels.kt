package com.example.neuro.api.model

// 文章列表项响应 (原 BookResponse)
data class ArticleIndex(
    val articleId: String,
    val title: String,
    val author: String,       // 作者名（字符串）
    val summary: String,      // 文章简介/摘要
    val wordCount: Int,       // 总字数
    val chapterCount: Int,    // 总章节数
    val tags: List<String>,   // 标签列表
    val lastUpdateTime: String
)

// 文章详情响应 (原 BookDetailResponse)
data class ArticleMeta(
    val articleId: String,
    val title: String,
    val author: String,       // 作者名（字符串）
    val summary: String,      // 摘要
    val tags: List<String>,
    val wordCount: Int,
    val chapterCount: Int,
    val status: String,       // published/draft
    val publishTime: String,
    val lastUpdateTime: String,
    val chapters: List<ArticleChapterMeta> // 章节列表
)

// 章节元数据（在文章详情中）
data class ArticleChapterMeta(
    val index: Int,           // 章节索引（从0开始）
    val chapterId: String,    // 章节ID
    val title: String,        // 章节标题
    val wordCount: Int        // 章节字数
)

// 章节内容响应
data class ChapterContentResponse(
    val chapterId: String,
    val bookId: String,       // 文章ID（字段名保持兼容）
    val title: String,
    val content: String,      // 完整正文内容
    val paragraphs: List<String>, // 按段落分割的数组，前端直接渲染
    val prevChapterId: Int?,  // 上一章索引（int），第一章为 null
    val nextChapterId: Int?,  // 下一章索引（int），最后一章为 null
    val paragraphComments: Map<Int, Int> // 保留字段，段评功能已移除
)
