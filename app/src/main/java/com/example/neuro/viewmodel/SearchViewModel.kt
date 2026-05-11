package com.example.neuro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuro.api.model.ArticleIndex
import com.example.neuro.repository.BookRepository
import com.example.neuro.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ArticleIndex>>(emptyList())
    val searchResults: StateFlow<List<ArticleIndex>> = _searchResults.asStateFlow()

    fun search(keyword: String, page: Int = 1) {
        if (keyword.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading

            when (val result = repository.getArticles(0, page, forceRefresh = true)) {
                is ApiResult.Success -> {
                    _searchResults.value = result.data
                    _uiState.value = SearchUiState.Success
                }
                is ApiResult.Error -> {
                    _uiState.value = SearchUiState.Error(result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }
}

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    object Success : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}
