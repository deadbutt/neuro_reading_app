package com.example.neuro.repository

import com.example.neuro.api.ApiService
import com.example.neuro.api.model.*
import com.example.neuro.util.ApiHelper
import com.example.neuro.util.ApiResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService
) {

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

    suspend fun forgotPassword(request: ForgotPasswordRequest): ApiResult<Unit> {
        return ApiHelper.safeApiCallWithMessage {
            apiService.forgotPassword(request)
        }
    }

    suspend fun refreshToken(request: RefreshTokenRequest): ApiResult<LoginResponse> {
        return ApiHelper.safeApiCallWithMessage {
            apiService.refreshToken(request)
        }
    }

    suspend fun followAuthor(authorId: String): ApiResult<Unit> {
        return ApiHelper.safeApiCallWithMessage {
            apiService.followAuthor(authorId)
        }
    }

    suspend fun unfollowAuthor(authorId: String): ApiResult<Unit> {
        return ApiHelper.safeApiCallWithMessage {
            apiService.unfollowAuthor(authorId)
        }
    }

    suspend fun getFollowingList(page: Int = 1, pageSize: Int = 20): ApiResult<PaginatedResponse<AuthorResponse>> {
        return ApiHelper.safeApiCallWithMessage {
            apiService.getFollowingList(page, pageSize)
        }
    }
}
