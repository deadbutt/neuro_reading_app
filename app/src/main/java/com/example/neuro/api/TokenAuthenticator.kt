package com.example.neuro.api

import com.example.neuro.NeuroApplication
import com.example.neuro.UserManager
import com.example.neuro.api.model.RefreshTokenRequest
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit

class TokenAuthenticator : Authenticator {

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(10L, TimeUnit.SECONDS)
        .readTimeout(10L, TimeUnit.SECONDS)
        .build()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.contains("/auth/refresh-token")) {
            return null
        }

        val context = NeuroApplication.getContext()
        val refreshToken = UserManager.getRefreshToken(context) ?: run {
            UserManager.clearLoginInfo(context)
            return null
        }

        val refreshRequest = RefreshTokenRequest(refreshToken)
        val requestBody = gson.toJson(refreshRequest).toRequestBody(jsonMediaType)

        val tokenUrl = response.request.url.newBuilder()
            .encodedPath("/api/v1/auth/refresh-token")
            .build()

        val request = Request.Builder()
            .url(tokenUrl)
            .post(requestBody)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .build()

        return try {
            val refreshResponse = refreshClient.newCall(request).execute()
            if (refreshResponse.isSuccessful) {
                val body = refreshResponse.body?.string()
                val result = gson.fromJson(body, RefreshTokenResult::class.java)
                if (result.code == 0 && result.data != null) {
                    val newToken = result.data.token
                    val newRefreshToken = result.data.refreshToken
                    UserManager.updateToken(context, newToken, newRefreshToken)

                    response.request.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()
                } else {
                    UserManager.clearLoginInfo(context)
                    null
                }
            } else {
                UserManager.clearLoginInfo(context)
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private data class RefreshTokenResult(
        val code: Int,
        val message: String,
        val data: RefreshTokenData?
    )

    private data class RefreshTokenData(
        @SerializedName("token") val token: String,
        @SerializedName("refreshToken") val refreshToken: String
    )
}
