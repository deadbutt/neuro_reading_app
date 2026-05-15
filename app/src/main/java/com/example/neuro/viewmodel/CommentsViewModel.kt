package com.example.neuro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuro.api.model.CommentResponse
import com.example.neuro.api.model.PostCommentRequest
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
class CommentsViewModel @Inject constructor(
    private val repository: ArticleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _comments = MutableStateFlow<List<CommentResponse>>(emptyList())
    val comments: StateFlow<List<CommentResponse>> = _comments.asStateFlow()

    private var currentSort: String = "hot"
    private var currentPage: Int = 1

    fun loadComments(articleId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            currentPage = 1

            when (val result = repository.getArticleComments(articleId, currentPage, sort = currentSort)) {
                is ApiResult.Success -> {
                    _comments.value = result.data.safeList()
                    _uiState.value = UiState.Success
                }
                is ApiResult.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }

    fun refreshComments(articleId: String) {
        loadComments(articleId)
    }

    fun setSort(sort: String) {
        currentSort = sort
        currentPage = 1
    }

    fun postComment(articleId: String, content: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val request = PostCommentRequest(content = content)
            when (val result = repository.postComment(articleId, request)) {
                is ApiResult.Success -> {
                    val newComment = result.data
                    _comments.value = listOf(newComment) + _comments.value
                    onResult(true, "评论已发送")
                }
                is ApiResult.Error -> {
                    onResult(false, result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }

    fun toggleLike(commentId: String, currentlyLiked: Boolean) {
        _comments.value = _comments.value.map {
            if (it.commentId == commentId) {
                it.copy(
                    isLiked = !currentlyLiked,
                    likeCount = if (currentlyLiked) it.likeCount - 1 else it.likeCount + 1
                )
            } else it
        }

        viewModelScope.launch {
            if (currentlyLiked) {
                repository.unlikeComment(commentId)
            } else {
                repository.likeComment(commentId)
            }
        }
    }
}
