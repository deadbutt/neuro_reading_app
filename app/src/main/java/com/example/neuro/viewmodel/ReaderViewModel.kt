package com.example.neuro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuro.api.model.ArticleChapterMeta
import com.example.neuro.api.model.ArticleMeta
import com.example.neuro.api.model.ChapterContentResponse
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
class ReaderViewModel @Inject constructor(
    private val repository: ArticleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _chapterContent = MutableStateFlow<ChapterContentResponse?>(null)
    val chapterContent: StateFlow<ChapterContentResponse?> = _chapterContent.asStateFlow()

    private val _chapters = MutableStateFlow<List<ArticleChapterMeta>>(emptyList())
    val chapters: StateFlow<List<ArticleChapterMeta>> = _chapters.asStateFlow()

    fun loadArticleMeta(articleId: String) {
        viewModelScope.launch {
            when (val result = repository.getArticleDetail(articleId)) {
                is ApiResult.Success -> {
                    _chapters.value = result.data.chapters
                }
                is ApiResult.Error -> {}
                ApiResult.Loading -> {}
            }
        }
    }

    fun loadChapter(articleId: String, chapterIndex: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            when (val result = repository.getChapterContent(articleId, chapterIndex)) {
                is ApiResult.Success -> {
                    _chapterContent.value = result.data
                    _uiState.value = UiState.Success
                }
                is ApiResult.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }
}
