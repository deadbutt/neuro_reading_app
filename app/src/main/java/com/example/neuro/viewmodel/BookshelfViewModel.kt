package com.example.neuro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuro.ShelfItem
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

    private var isEditMode = false

    fun loadBookshelf() {
        viewModelScope.launch {
            _uiState.value = BookshelfUiState.Loading

            when (val result = articleRepository.getBookshelf()) {
                is ApiResult.Success -> {
                    val shelfItems = result.data.safeList().map { item ->
                        ShelfItem(
                            bookId = item.articleId,
                            title = item.title,
                            author = item.author,
                            tag = "书架",
                            progress = item.progress,
                            coverUrl = item.cover,
                            lastReadChapter = item.lastReadChapter,
                            chapterIndex = item.chapterIndex,
                            isFinished = item.isFinished
                        )
                    }
                    _books.value = shelfItems
                    _uiState.value = if (shelfItems.isEmpty()) BookshelfUiState.Empty else BookshelfUiState.Success
                }
                is ApiResult.Error -> {
                    _uiState.value = BookshelfUiState.Error(result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }

    fun removeBooks(selectedBooks: List<ShelfItem>) {
        if (selectedBooks.isEmpty()) return

        viewModelScope.launch {
            var successCount = 0
            for (book in selectedBooks) {
                when (val result = articleRepository.removeFromBookshelf(book.bookId)) {
                    is ApiResult.Success -> successCount++
                    is ApiResult.Error -> {}
                    ApiResult.Loading -> {}
                }
            }
            _books.value = _books.value.filter { it !in selectedBooks }
            _uiState.value = BookshelfUiState.RemoveResult(successCount, selectedBooks.size)
        }
    }

    fun toggleSelectAll(): Boolean {
        val allSelected = _books.value.all { it.isSelected }
        _books.value = _books.value.map { it.copy(isSelected = !allSelected) }
        return !allSelected
    }

    fun getSelectedBooks(): List<ShelfItem> {
        return _books.value.filter { it.isSelected }
    }
}

sealed class BookshelfUiState {
    object Idle : BookshelfUiState()
    object Loading : BookshelfUiState()
    object Success : BookshelfUiState()
    object Empty : BookshelfUiState()
    data class Error(val message: String) : BookshelfUiState()
    data class RemoveResult(val successCount: Int, val totalCount: Int) : BookshelfUiState()
}
