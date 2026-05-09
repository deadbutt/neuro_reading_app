package com.example.neuro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuro.api.model.ChapterContentResponse
import com.example.neuro.repository.ArticleRepository
import com.example.neuro.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: ArticleRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Idle)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()
    
    private val _chapterContent = MutableStateFlow<ChapterContentResponse?>(null)
    val chapterContent: StateFlow<ChapterContentResponse?> = _chapterContent.asStateFlow()
    
    fun loadChapter(articleId: String, chapterIndex: Int) {
        viewModelScope.launch {
            _uiState.value = ReaderUiState.Loading
            
            when (val result = repository.getChapterContent(articleId, chapterIndex)) {
                is ApiResult.Success -> {
                    _chapterContent.value = result.data
                    _uiState.value = ReaderUiState.Success
                }
                is ApiResult.Error -> {
                    _uiState.value = ReaderUiState.Error(result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }
}

sealed class ReaderUiState {
    object Idle : ReaderUiState()
    object Loading : ReaderUiState()
    object Success : ReaderUiState()
    data class Error(val message: String) : ReaderUiState()
}
