package com.example.neuro.base

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    object Success : UiState()
    object Empty : UiState()
    data class Error(val message: String) : UiState()
}
