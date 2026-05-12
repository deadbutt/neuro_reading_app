package com.example.neuro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuro.BookItem
import com.example.neuro.base.UiState
import com.example.neuro.repository.BookRepository
import com.example.neuro.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<BookItem>>(emptyList())
    val searchResults: StateFlow<List<BookItem>> = _searchResults.asStateFlow()

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    private var searchJob: Job? = null

    fun search(keyword: String) {
        if (keyword.isBlank()) {
            _searchResults.value = emptyList()
            _uiState.value = UiState.Idle
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.value = UiState.Loading

            when (val result = repository.searchArticles(keyword)) {
                is ApiResult.Success -> {
                    val items = result.data.map { article ->
                        BookItem(
                            bookId = article.articleId,
                            title = article.title,
                            author = article.author,
                            desc = article.summary,
                            coverUrl = article.cover ?: ""
                        )
                    }
                    _searchResults.value = items
                    _uiState.value = if (items.isEmpty()) UiState.Empty else UiState.Success
                }
                is ApiResult.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }

    fun updateHistory(query: String) {
        if (query.isBlank()) return
        val list = _history.value.toMutableList()
        list.remove(query)
        list.add(0, query)
        if (list.size > MAX_HISTORY_SIZE) {
            list.subList(MAX_HISTORY_SIZE, list.size).clear()
        }
        _history.value = list
    }

    fun clearHistory() {
        _history.value = emptyList()
    }

    fun setHistory(history: List<String>) {
        _history.value = history
    }

    companion object {
        const val MAX_HISTORY_SIZE = 10
    }
}
