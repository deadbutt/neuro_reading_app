package com.example.neuro.api.model

// 评论响应
data class CommentResponse(
    val commentId: String,
    val articleId: String? = null,    // 文章ID
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val content: String,
    val createTime: String,
    val likeCount: Int,
    val isLiked: Boolean,
    val replyCount: Int,
    val replies: List<CommentReplyResponse>?
)

// 评论回复
data class CommentReplyResponse(
    val replyId: String,
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val content: String,
    val createTime: String,
    val toUserName: String?  // 回复给哪位用户
)

// 发表评论请求
data class PostCommentRequest(
    val content: String,
    val parentId: String? = null  // 回复某条评论时使用
)


