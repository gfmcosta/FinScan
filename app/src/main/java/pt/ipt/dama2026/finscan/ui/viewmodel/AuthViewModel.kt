package pt.ipt.dama2026.finscan.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pt.ipt.dama2026.finscan.data.api.services.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(context: Context) : ViewModel() {
    private val authService = AuthService(context)

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val token: String) : LoginState()
        data class Error(val message: String) : LoginState()
    }

    sealed class RegisterState {
        object Idle : RegisterState()
        object Loading : RegisterState()
        object Success : RegisterState()
        data class Error(val message: String) : RegisterState()
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val result = authService.login(username, password)
            _loginState.value = when (result) {
                is AuthService.Result.Success -> LoginState.Success(result.data)
                is AuthService.Result.Error -> LoginState.Error(result.message)
                else -> LoginState.Error("Unknown error")
            }
        }
    }

    fun register(username: String, name: String, email: String, password: String) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            val result = authService.register(username, name, email, password)
            _registerState.value = when (result) {
                is AuthService.Result.Success -> RegisterState.Success
                is AuthService.Result.Error -> RegisterState.Error(result.message)
                else -> RegisterState.Error("Unknown error")
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }

    fun resetRegisterState() {
        _registerState.value = RegisterState.Idle
    }
}
