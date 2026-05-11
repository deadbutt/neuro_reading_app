package com.example.neuro.util

import com.example.neuro.Constants
import com.example.neuro.api.model.BaseResponse
import retrofit2.Response

object ApiHelper {

    suspend fun <T> safeApiCall(
        apiCall: suspend () -> Response<BaseResponse<T>>
    ): Result<T> {
        return try {
            val response = apiCall()

            if (response.isSuccessful && response.body()?.code == Constants.ApiCode.SUCCESS) {
                val data = response.body()?.data
                if (data != null) {
                    Result.success(data)
                } else {
                    Result.failure(Exception("数据为空"))
                }
            } else {
                val message = response.body()?.message ?: "请求失败"
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun <T> safeApiCallWithMessage(
        apiCall: suspend () -> Response<BaseResponse<T>>
    ): ApiResult<T> {
        return try {
            val response = apiCall()

            if (response.isSuccessful && response.body()?.code == Constants.ApiCode.SUCCESS) {
                val data = response.body()?.data
                if (data != null) {
                    ApiResult.Success(data)
                } else {
                    ApiResult.Error("数据为空")
                }
            } else {
                val message = response.body()?.message ?: "请求失败"
                ApiResult.Error(message)
            }
        } catch (e: Exception) {
            ApiResult.Error("网络错误：${e.message}")
        }
    }
}

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}
