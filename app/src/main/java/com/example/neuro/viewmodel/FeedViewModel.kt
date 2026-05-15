package com.example.neuro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuro.api.ApiService
import com.example.neuro.api.model.FeedActivityResponse
import com.example.neuro.api.model.PaginatedResponse
import com.example.neuro.base.UiState
import com.example.neuro.util.ApiHelper
import com.example.neuro.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _feedItems = MutableStateFlow<List<FeedActivityResponse>>(emptyList())
    val feedItems: StateFlow<List<FeedActivityResponse>> = _feedItems.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private var currentPage = 1

    fun loadFeed(isRefresh: Boolean = false) {
        if (isRefresh) {
            currentPage = 1
            _hasMore.value = true
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading

            when (val result = safeFeedApiCall { apiService.getFeedList(page = currentPage) }) {
                is ApiResult.Success -> {
                    val items = result.data.safeList()
                    if (isRefresh) {
                        _feedItems.value = items
                    } else {
                        _feedItems.value = _feedItems.value + items
                    }
                    _hasMore.value = items.isNotEmpty()
                    if (items.isNotEmpty()) currentPage++
                    _uiState.value = if (_feedItems.value.isEmpty()) UiState.Empty else UiState.Success
                }
                is ApiResult.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }

    private suspend fun <T> safeFeedApiCall(
        apiCall: suspend () -> retrofit2.Response<com.example.neuro.api.model.BaseResponse<T>>
    ): ApiResult<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful && response.body()?.code == 0) {
                val data = response.body()?.data
                if (data != null) ApiResult.Success(data)
                else ApiResult.Error("数据为空")
            } else {
                ApiResult.Error(response.body()?.message ?: "请求失败")
            }
        } catch (e: Exception) {
            ApiResult.Error("网络错误：${e.message}")
        }
    }
}
