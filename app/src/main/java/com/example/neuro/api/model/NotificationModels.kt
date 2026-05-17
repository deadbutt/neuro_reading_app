package com.example.neuro.api.model

data class NotificationResponse(
    val notificationId: String,
    val type: String,
    val title: String,
    val content: String,
    val relatedId: String,
    val fromUserId: String,
    val fromUserName: String,
    val fromUserAvatar: String,
    val isRead: Boolean,
    val createTime: String
)

data class NotificationCountResponse(
    val unreadCount: Long,
    val totalCount: Long
)

data class FavoriteStatusResponse(
    val isFavorite: Boolean
)