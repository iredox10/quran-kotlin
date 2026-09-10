package com.nur.quran.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nur.quran.data.sync.AppwriteClient
import dagger.hilt.android.lifecycle.HiltViewModel
import io.appwrite.ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val signedIn: Boolean = false,
    val email: String? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val appwrite: AppwriteClient
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        refresh()
    }

    fun isPasswordValid(pw: String) = pw.length >= 8

    fun login(email: String, password: String) {
        if (!isPasswordValid(password)) {
            _authState.value = _authState.value.copy(
                busy = false,
                error = "Password must be at least 8 characters",
                message = null
            )
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _authState.value = _authState.value.copy(busy = true, error = null, message = null)
            runCatching {
                appwrite.account.createSession(email.trim(), password)
                appwrite.account.get()
            }.onSuccess { user ->
                _authState.value = AuthState(signedIn = true, email = user.email, busy = false, error = null)
            }.onFailure { e ->
                _authState.value = _authState.value.copy(busy = false, error = e.message ?: "Login failed")
            }
        }
    }

    fun register(email: String, password: String) {
        if (!isPasswordValid(password)) {
            _authState.value = _authState.value.copy(
                busy = false,
                error = "Password must be at least 8 characters",
                message = null
            )
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _authState.value = _authState.value.copy(busy = true, error = null, message = null)
            runCatching {
                appwrite.account.create(ID.unique(), email.trim(), password)
                appwrite.account.createSession(email.trim(), password)
                appwrite.account.get()
            }.onSuccess { user ->
                _authState.value = AuthState(signedIn = true, email = user.email, busy = false, error = null)
            }.onFailure { e ->
                _authState.value = _authState.value.copy(busy = false, error = e.message ?: "Registration failed")
            }
        }
    }

    fun sendRecovery(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _authState.value = _authState.value.copy(busy = true, error = null, message = null)
            runCatching {
                appwrite.account.createRecovery(
                    email.trim(),
                    "https://quran-nur.appwrite.network/profile"
                )
            }.onSuccess {
                _authState.value = _authState.value.copy(
                    busy = false,
                    error = null,
                    message = "Recovery email sent — check your inbox"
                )
            }.onFailure { e ->
                _authState.value = _authState.value.copy(
                    busy = false,
                    error = e.message ?: "Recovery failed"
                )
            }
        }
    }

    fun clearMessage() {
        _authState.value = _authState.value.copy(message = null)
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            _authState.value = _authState.value.copy(busy = true, error = null, message = null)
            runCatching {
                appwrite.account.deleteSession("current")
            }.onSuccess {
                _authState.value = AuthState(signedIn = false, email = null, busy = false, error = null)
            }.onFailure { e ->
                _authState.value = _authState.value.copy(busy = false, error = e.message ?: "Logout failed")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                appwrite.account.get()
            }.onSuccess { user ->
                _authState.value = AuthState(signedIn = true, email = user.email, busy = false, error = null)
            }.onFailure {
                _authState.value = AuthState(signedIn = false, email = null, busy = false, error = null)
            }
        }
    }
}
