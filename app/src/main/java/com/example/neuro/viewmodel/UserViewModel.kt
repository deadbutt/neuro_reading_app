package com.example.neuro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuro.api.model.LoginResponse
import com.example.neuro.api.model.UserProfileResponse
import com.example.neuro.repository.UserRepository
import com.example.neuro.util.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {
    
    private val repository = UserRepository()
    
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()
    
    private val _userProfile = MutableStateFlow<UserProfileResponse?>(null)
    val userProfile: StateFlow<UserProfileResponse?> = _userProfile.asStateFlow()
    
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
