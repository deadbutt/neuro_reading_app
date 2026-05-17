package com.example.neuro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuro.NotificationItem
import com.example.neuro.api.model.NotificationResponse
import com.example.neuro.repository.UserRepository
import com.example.neuro.toNotificationItem
import com.example.neuro.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Idle)
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = NotificationUiState.Loading

            when (val result = repository.getNotifications()) {
                is ApiResult.Success -> {
                    val items = result.data?.safeList()?.map { it.toNotificationItem() } ?: emptyList()
                    _notifications.value = items
                    _uiState.value = if (items.isEmpty()) NotificationUiState.Empty else NotificationUiState.Success
                }
                is ApiResult.Error -> {
                    _uiState.value = NotificationUiState.Error(result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            when (val result = repository.markNotificationAsRead(notificationId)) {
                is ApiResult.Success -> {
                    _notifications.value = _notifications.value.map {
                        if (it.notificationId == notificationId) it.copy(isRead = true) else it
                    }
                }
                is ApiResult.Error -> {}
                ApiResult.Loading -> {}
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            when (val result = repository.markAllNotificationsAsRead()) {
                is ApiResult.Success -> {
                    _notifications.value = _notifications.value.map { it.copy(isRead = true) }
                }
                is ApiResult.Error -> {}
                ApiResult.Loading -> {}
            }
        }
    }

    sealed class NotificationUiState {
        object Idle : NotificationUiState()
        object Loading : NotificationUiState()
        object Success : NotificationUiState()
        object Empty : NotificationUiState()
        data class Error(val message: String) : NotificationUiState()
    }
}