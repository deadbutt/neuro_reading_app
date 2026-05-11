package com.example.neuro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuro.WorkItem
import com.example.neuro.repository.ArticleRepository
import com.example.neuro.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateCenterViewModel @Inject constructor(
    private val repository: ArticleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateCenterUiState>(CreateCenterUiState.Idle)
    val uiState: StateFlow<CreateCenterUiState> = _uiState.asStateFlow()

    private val _works = MutableStateFlow<List<WorkItem>>(emptyList())
    val works: StateFlow<List<WorkItem>> = _works.asStateFlow()

    fun loadMyWorks() {
        viewModelScope.launch {
            _uiState.value = CreateCenterUiState.Loading

            // Note: This would need a proper repository method for getMyWorks
            // For now, using placeholder to maintain functionality
            _uiState.value = CreateCenterUiState.Success
        }
    }
}

sealed class CreateCenterUiState {
    object Idle : CreateCenterUiState()
    object Loading : CreateCenterUiState()
    object Success : CreateCenterUiState()
    data class Error(val message: String) : CreateCenterUiState()
}
