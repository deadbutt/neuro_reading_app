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
class BookListViewModel @Inject constructor(
    private val repository: BookRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<BookListUiState>(BookListUiState.Idle)
    val uiState: StateFlow<BookListUiState> = _uiState.asStateFlow()
    
    private val _books = MutableStateFlow<List<ArticleIndex>>(emptyList())
    val books: StateFlow<List<ArticleIndex>> = _books.asStateFlow()
    
    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()
    
    private var currentPage = 1
    
    fun loadBooks(type: Int, isRefresh: Boolean = false) {
        if (isRefresh) {
            currentPage = 1
            _hasMore.value = true
        }
        
        viewModelScope.launch {
            _uiState.value = BookListUiState.Loading
            
            when (val result = repository.getArticles(type, currentPage, forceRefresh = isRefresh)) {
                is ApiResult.Success -> {
                    val newBooks = result.data
                    if (isRefresh) {
                        _books.value = newBooks
                    } else {
                        _books.value = _books.value + newBooks
                    }
                    _hasMore.value = newBooks.isNotEmpty()
                    if (newBooks.isNotEmpty()) {
                        currentPage++
                    }
                    _uiState.value = BookListUiState.Success
                }
                is ApiResult.Error -> {
                    _uiState.value = BookListUiState.Error(result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }
}

sealed class BookListUiState {
    object Idle : BookListUiState()
    object Loading : BookListUiState()
    object Success : BookListUiState()
    data class Error(val message: String) : BookListUiState()
}
