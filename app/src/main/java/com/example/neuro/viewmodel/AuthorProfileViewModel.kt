package com.example.neuro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuro.api.model.ArticleIndex
import com.example.neuro.api.model.AuthorProfileResponse
import com.example.neuro.base.UiState
import com.example.neuro.repository.UserRepository
import com.example.neuro.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthorProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _authorProfile = MutableStateFlow<AuthorProfileResponse?>(null)
    val authorProfile: StateFlow<AuthorProfileResponse?> = _authorProfile.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    private val _works = MutableStateFlow<List<ArticleIndex>>(emptyList())
    val works: StateFlow<List<ArticleIndex>> = _works.asStateFlow()

    fun loadAuthorProfile(authorId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            when (val result = userRepository.getAuthorProfile(authorId)) {
                is ApiResult.Success -> {
                    _authorProfile.value = result.data
                    _isFollowing.value = result.data.isFollowing
                    _uiState.value = UiState.Success
                }
                is ApiResult.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }

    fun loadWorks(authorId: String, page: Int = 1) {
        viewModelScope.launch {
            when (val result = userRepository.getAuthorWorks(authorId, page)) {
                is ApiResult.Success -> {
                    val list = result.data.safeList()
                    if (page == 1) {
                        _works.value = list
                    } else {
                        _works.value = _works.value + list
                    }
                }
                is ApiResult.Error -> {}
                ApiResult.Loading -> {}
            }
        }
    }

    fun toggleFollow(authorId: String) {
        viewModelScope.launch {
            val current = _isFollowing.value
            val result = if (current) {
                userRepository.unfollowAuthor(authorId)
            } else {
                userRepository.followAuthor(authorId)
            }

            when (result) {
                is ApiResult.Success -> {
                    _isFollowing.value = !current
                }
                is ApiResult.Error -> {}
                ApiResult.Loading -> {}
            }
        }
    }
}
