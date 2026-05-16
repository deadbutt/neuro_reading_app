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

    private val _allChapterContents = MutableStateFlow<Map<Int, ChapterContentResponse>>(emptyMap())
    val allChapterContents: StateFlow<Map<Int, ChapterContentResponse>> = _allChapterContents.asStateFlow()

    private val _chapters = MutableStateFlow<List<ArticleChapterMeta>>(emptyList())
    val chapters: StateFlow<List<ArticleChapterMeta>> = _chapters.asStateFlow()

    private val _totalWordCount = MutableStateFlow(0)
    val totalWordCount: StateFlow<Int> = _totalWordCount.asStateFlow()

    fun loadArticleMeta(articleId: String) {
        viewModelScope.launch {
            when (val result = repository.getArticleDetail(articleId)) {
                is ApiResult.Success -> {
                    val meta = result.data
                    _chapters.value = meta?.chapters ?: emptyList()
                    _totalWordCount.value = meta?.wordCount ?: 0
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
                    result.data?.let { _chapterContent.value = it }
                    _uiState.value = UiState.Success
                }
                is ApiResult.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }

    fun loadAllChapters(articleId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val chapterList = _chapters.value
            val allContents = mutableMapOf<Int, ChapterContentResponse>()

            for ((index, chapter) in chapterList.withIndex()) {
                when (val result = repository.getChapterContent(articleId, chapter.index)) {
                    is ApiResult.Success -> {
                        result.data?.let { allContents[index] = it }
                    }
                    is ApiResult.Error -> {
                        _uiState.value = UiState.Error(result.message)
                        return@launch
                    }
                    ApiResult.Loading -> {}
                }
            }

            _allChapterContents.value = allContents
            _uiState.value = UiState.Success
        }
    }

    fun saveProgress(articleId: String, chapterIndex: Int, progress: Int, position: Int) {
        viewModelScope.launch {
            repository.updateReadingProgress(articleId, chapterIndex, progress, position)
        }
    }
}
