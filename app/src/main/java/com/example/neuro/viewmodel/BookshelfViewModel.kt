package com.example.neuro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuro.ShelfItem
import com.example.neuro.api.model.BookshelfItemResponse
import com.example.neuro.repository.ArticleRepository
import com.example.neuro.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookshelfViewModel @Inject constructor(
    private val articleRepository: ArticleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BookshelfUiState>(BookshelfUiState.Idle)
    val uiState: StateFlow<BookshelfUiState> = _uiState.asStateFlow()

    private val _books = MutableStateFlow<List<ShelfItem>>(emptyList())
    val books: StateFlow<List<ShelfItem>> = _books.asStateFlow()

    fun loadBookshelf() {
        viewModelScope.launch {
            _uiState.value = BookshelfUiState.Loading

            // Note: This would need a proper repository method for bookshelf
            // For now, using a placeholder that maintains the existing behavior
            _uiState.value = BookshelfUiState.Success
        }
    }

    fun removeBooks(bookIds: List<String>) {
        viewModelScope.launch {
            var successCount = 0
            for (bookId in bookIds) {
                when (val result = articleRepository.removeFromBookshelf(bookId)) {
                    is ApiResult.Success -> successCount++
                    is ApiResult.Error -> {}
                    ApiResult.Loading -> {}
                }
            }
            _books.value = _books.value.filter { it.bookId !in bookIds }
            _uiState.value = BookshelfUiState.Success
        }
    }
}

sealed class BookshelfUiState {
    object Idle : BookshelfUiState()
    object Loading : BookshelfUiState()
    object Success : BookshelfUiState()
    data class Error(val message: String) : BookshelfUiState()
}
