package com.example.neuro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuro.api.model.ArticleMeta
import com.example.neuro.repository.ArticleRepository
import com.example.neuro.util.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookDetailViewModel : ViewModel() {
    
    private val repository = ArticleRepository()
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _article = MutableStateFlow<ArticleMeta?>(null)
    val article: StateFlow<ArticleMeta?> = _article.asStateFlow()
    
    fun loadArticleDetail(articleId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            when (val result = repository.getArticleDetail(articleId)) {
                is ApiResult.Success -> {
                    _article.value = result.data
                    _uiState.value = UiState.Success
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

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    object Success : UiState()
    data class Error(val message: String) : UiState()
}
