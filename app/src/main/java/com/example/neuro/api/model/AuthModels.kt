package com.example.neuro.api.model

// 发送验证码请求
data class SendCodeRequest(
    val account: String,      // QQ号或邮箱
    val type: String          // "qq" 或 "email"
)

// 登录请求
data class LoginRequest(
    val account: String,      // QQ号或邮箱
    val password: String      // 密码（前端MD5加密后传输）
)

// 注册请求
data class RegisterRequest(
    val account: String,      // QQ号或邮箱
    val code: String,         // 验证码
    val password: String,     // 密码（前端MD5加密后传输）
    val confirmPassword: String,
    val nickname: String? = null  // 用户昵称，可选，不传则后端自动生成
)

// 忘记密码请求
data class ForgotPasswordRequest(
    val account: String,      // QQ号或邮箱
    val code: String,         // 验证码
    val newPassword: String   // 新密码（前端MD5加密后传输）
)

// 刷新Token请求
data class RefreshTokenRequest(
    val refreshToken: String
)

// 登录响应
data class LoginResponse(
    val userId: String,
    val account: String,
    val nickname: String,
    val avatar: String,
    val token: String,        // Access Token
    val refreshToken: String, // Refresh Token
    val expiresIn: Long       // Token过期时间（秒）
)
