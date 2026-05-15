package com.example.neuro.api.model

// 通用响应包装
data class BaseResponse<T>(
    val code: Int,            // 业务状态码 0=成功
    val message: String,      // 提示信息
    val data: T?              // 响应数据
)

// 分页响应包装
data class PaginatedResponse<T>(
    val list: List<T>? = null,              // 数据列表（后端可能返回null）
    val total: Int = 0,                     // 总条数
    val page: Int = 1,                      // 当前页
    val pageSize: Int = 20,                 // 每页条数
    val hasMore: Boolean = false            // 是否还有更多
) {
    fun safeList(): List<T> = list ?: emptyList()
}

// 作者简要信息
data class AuthorResponse(
    val authorId: String,
    val name: String,
    val avatar: String,
    val description: String
)

// 头像上传响应
data class UploadAvatarResponse(
    val filename: String,
    val url: String
)
