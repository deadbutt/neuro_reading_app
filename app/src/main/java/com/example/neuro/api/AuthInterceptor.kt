package com.example.neuro.api

import com.example.neuro.NeuroApplication
import com.example.neuro.UserManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()

        // 添加通用Header
        builder.addHeader("Accept", "application/json")
        builder.addHeader("Content-Type", "application/json")

        // 从UserManager获取token并添加到请求头
        val token = UserManager.getToken(NeuroApplication.getContext())
        if (!token.isNullOrEmpty()) {
            builder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(builder.build())
    }
}
