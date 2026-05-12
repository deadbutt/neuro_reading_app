package com.example.neuro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuro.WorkItem
import com.example.neuro.base.UiState
import com.example.neuro.api.RetrofitClient
import com.example.neuro.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateCenterViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _works = MutableStateFlow<List<WorkItem>>(emptyList())
    val works: StateFlow<List<WorkItem>> = _works.asStateFlow()

    fun loadMyWorks(status: String = "all", page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            try {
                val response = RetrofitClient.apiService.getMyWorks(page = page, status = status)
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()?.data
                    val items = data?.list?.map { work ->
                        WorkItem(
                            articleId = work.articleId,
                            title = work.title,
                            summary = work.summary,
                            cover = work.cover ?: "",
                            status = work.status ?: "",
                            chapterCount = work.chapterCount,
                            wordCount = work.wordCount,
                            lastUpdateTime = work.lastUpdateTime
                        )
                    } ?: emptyList()

                    if (page == 1) {
                        _works.value = items
                    } else {
                        _works.value = _works.value + items
                    }
                    _uiState.value = if (_works.value.isEmpty()) UiState.Empty else UiState.Success
                } else {
                    val message = response.body()?.message ?: "加载失败"
                    _uiState.value = UiState.Error(message)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("网络错误：${e.message}")
            }
        }
    }

    fun deleteWork(workId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteWork(workId)
                if (response.isSuccessful && response.body()?.code == 0) {
                    _works.value = _works.value.filter { it.articleId != workId }
                    onResult(true, "删除成功")
                } else {
                    val message = response.body()?.message ?: "删除失败"
                    onResult(false, message)
                }
            } catch (e: Exception) {
                onResult(false, "网络错误：${e.message}")
            }
        }
    }
}
