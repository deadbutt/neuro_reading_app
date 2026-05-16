package com.example.neuro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuro.api.model.ArticleMeta
import com.example.neuro.api.model.CommentResponse
import com.example.neuro.base.UiState
import com.example.neuro.repository.ArticleRepository
import com.example.neuro.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    private val repository: ArticleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _article = MutableStateFlow<ArticleMeta?>(null)
    val article: StateFlow<ArticleMeta?> = _article.asStateFlow()

    private val _previewComments = MutableStateFlow<List<CommentResponse>>(emptyList())
    val previewComments: StateFlow<List<CommentResponse>> = _previewComments.asStateFlow()

    fun loadArticleDetail(articleId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            when (val result = repository.getArticleDetail(articleId)) {
                is ApiResult.Success -> {
                    result.data?.let { _article.value = it }
                    _uiState.value = UiState.Success
                    loadPreviewComments(articleId)
                }
                is ApiResult.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
                ApiResult.Loading -> {
                    _uiState.value = UiState.Loading
                }
            }
        }
    }

    private fun loadPreviewComments(articleId: String) {
        viewModelScope.launch {
            when (val result = repository.getArticleComments(articleId, page = 1, pageSize = 3, sort = "hot")) {
                is ApiResult.Success -> {
                    _previewComments.value = result.data?.safeList()?.take(3) ?: emptyList()
                }
                is ApiResult.Error -> {
                    _previewComments.value = emptyList()
                }
                ApiResult.Loading -> {}
            }
        }
    }

    fun addToBookshelf(articleId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = repository.addToBookshelf(articleId)) {
                is ApiResult.Success -> {
                    onSuccess()
                }
                is ApiResult.Error -> {
                    onError(result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }
}
