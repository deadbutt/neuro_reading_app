package com.example.neuro.api.model

// 通用响应包装
data class BaseResponse<T>(
    val code: Int,            // 业务状态码 0=成功
    val message: String,      // 提示信息
    val data: T?              // 响应数据
)

// 分页响应包装
data class PaginatedResponse<T>(
    val list: List<T>,        // 数据列表
    val total: Int,           // 总条数
    val page: Int,            // 当前页
    val pageSize: Int,        // 每页条数
    val hasMore: Boolean      // 是否还有更多
)

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

// 作品上传响应
data class UploadWorkResponse(
    val workId: String,
    val title: String,
    val status: String
)
