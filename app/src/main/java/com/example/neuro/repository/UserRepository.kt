package com.example.neuro.repository

import com.example.neuro.api.RetrofitClient
import com.example.neuro.api.model.*
import com.example.neuro.util.ApiHelper
import com.example.neuro.util.ApiResult

class UserRepository {
    
    private val apiService = RetrofitClient.apiService
    
    suspend fun login(account: String, password: String): ApiResult<LoginResponse> {
        return ApiHelper.safeApiCallWithMessage {
            apiService.login(LoginRequest(account, password))
        }
    }
    
    suspend fun register(
        account: String,
        code: String,
        password: String,
        confirmPassword: String,
        nickname: String?
    ): ApiResult<LoginResponse> {
        return ApiHelper.safeApiCallWithMessage {
            apiService.register(
                RegisterRequest(
                    account = account,
                    code = code,
                    password = password,
                    confirmPassword = confirmPassword,
                    nickname = nickname
                )
            )
        }
    }
    
    suspend fun sendVerificationCode(account: String, type: String): ApiResult<Unit> {
        return ApiHelper.safeApiCallWithMessage {
            apiService.sendVerificationCode(SendCodeRequest(account, type))
        }
    }
    
    suspend fun getUserProfile(): ApiResult<UserProfileResponse> {
        return ApiHelper.safeApiCallWithMessage {
            apiService.getUserProfile()
        }
    }
    
    suspend fun updateUserProfile(request: UpdateProfileRequest): ApiResult<UserProfileResponse> {
        return ApiHelper.safeApiCallWithMessage {
            apiService.updateUserProfile(request)
        }
    }
}
