package com.example.neuro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuro.api.model.LoginResponse
import com.example.neuro.api.model.ReadingHistoryResponse
import com.example.neuro.api.model.UserProfileResponse
import com.example.neuro.repository.UserRepository
import com.example.neuro.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfileResponse?>(null)
    val userProfile: StateFlow<UserProfileResponse?> = _userProfile.asStateFlow()

    private val _readingHistory = MutableStateFlow<List<ReadingHistoryResponse>>(emptyList())
    val readingHistory: StateFlow<List<ReadingHistoryResponse>> = _readingHistory.asStateFlow()

    private val _historyLoading = MutableStateFlow(false)
    val historyLoading: StateFlow<Boolean> = _historyLoading.asStateFlow()

    fun login(account: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            when (val result = repository.login(account, password)) {
                is ApiResult.Success -> {
                    _loginState.value = LoginState.Success(result.data)
                }
                is ApiResult.Error -> {
                    _loginState.value = LoginState.Error(result.message)
                }
                ApiResult.Loading -> {
                    _loginState.value = LoginState.Loading
                }
            }
        }
    }

    fun register(
        account: String,
        code: String,
        password: String,
        confirmPassword: String,
        nickname: String?
    ) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            when (val result = repository.register(account, code, password, confirmPassword, nickname)) {
                is ApiResult.Success -> {
                    _loginState.value = LoginState.Success(result.data)
                }
                is ApiResult.Error -> {
                    _loginState.value = LoginState.Error(result.message)
                }
                ApiResult.Loading -> {
                    _loginState.value = LoginState.Loading
                }
            }
        }
    }

    fun sendVerificationCode(account: String, type: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val result = repository.sendVerificationCode(account, type)) {
                is ApiResult.Success -> {
                    onResult(true, "验证码已发送")
                }
                is ApiResult.Error -> {
                    onResult(false, result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }

    fun getUserProfile() {
        viewModelScope.launch {
            when (val result = repository.getUserProfile()) {
                is ApiResult.Success -> {
                    _userProfile.value = result.data
                }
                is ApiResult.Error -> {
                    // 静默失败
                }
                ApiResult.Loading -> {}
            }
        }
    }

    fun getReadingHistory() {
        viewModelScope.launch {
            _historyLoading.value = true
            when (val result = repository.getReadingHistory()) {
                is ApiResult.Success -> {
                    _readingHistory.value = result.data?.safeList() ?: emptyList()
                }
                is ApiResult.Error -> {
                    // 静默失败
                }
                ApiResult.Loading -> {}
            }
            _historyLoading.value = false
        }
    }

    fun deleteReadingHistory(historyId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val result = repository.deleteReadingHistory(historyId)) {
                is ApiResult.Success -> {
                    _readingHistory.value = _readingHistory.value.filter { it.historyId != historyId }
                    onResult(true, "删除成功")
                }
                is ApiResult.Error -> {
                    onResult(false, result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }

    fun clearReadingHistory(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val result = repository.clearReadingHistory()) {
                is ApiResult.Success -> {
                    _readingHistory.value = emptyList()
                    onResult(true, "已清空")
                }
                is ApiResult.Error -> {
                    onResult(false, result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val data: LoginResponse) : LoginState()
    data class Error(val message: String) : LoginState()
}
