package com.nur.quran.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nur.quran.data.sync.AppwriteClient
import com.nur.quran.data.sync.SyncService
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val displayName: String? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val appwrite: AppwriteClient,
    private val syncService: SyncService,
    @ApplicationContext private val context: Context
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
                // NOTE: createSession(userId, secret) is the token/OTP endpoint —
                // email login must use createEmailPasswordSession (as the web app does).
                appwrite.account.createEmailPasswordSession(email.trim(), password)
                appwrite.account.get()
            }.onSuccess { user ->
                resetSyncWatermark()
                _authState.value = AuthState(
                    signedIn = true,
                    email = user.email,
                    displayName = user.name.takeIf { it.isNotBlank() },
                    busy = false,
                    error = null
                )
                // Web CloudSync.jsx (login transition): reset watermark then pull
                // immediately so a fresh device restores its cloud data right after
                // sign-in instead of waiting for the next periodic sync.
                restoreFromCloud()
            }.onFailure { e ->
                _authState.value = _authState.value.copy(busy = false, error = e.message ?: "Login failed")
            }
        }
    }

    /**
     * Pull-only restore after sign-in (CloudSync.jsx performPull). Runs in the
     * background and never blocks the auth UI; the watermark gate in
     * [SyncService.pull] makes a stale/no-op pull harmless.
     */
    private fun restoreFromCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { syncService.pull() }
        }
    }

    fun register(email: String, password: String, name: String = "") {
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
                appwrite.account.create(ID.unique(), email.trim(), password, name.trim())
                appwrite.account.createEmailPasswordSession(email.trim(), password)
                appwrite.account.get()
            }.onSuccess { user ->
                resetSyncWatermark()
                _authState.value = AuthState(
                    signedIn = true,
                    email = user.email,
                    displayName = user.name.takeIf { it.isNotBlank() },
                    busy = false,
                    error = null
                )
                restoreFromCloud()
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
                resetSyncWatermark()
                _authState.value = AuthState(signedIn = false, email = null, busy = false, error = null)
            }.onFailure { e ->
                _authState.value = _authState.value.copy(busy = false, error = e.message ?: "Logout failed")
            }
        }
    }

    private fun resetSyncWatermark() {
        runCatching {
            context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_SYNC_AT, 0L)
                .apply()
        }
    }

    private companion object {
        const val SYNC_PREFS = "sync_state"
        const val KEY_LAST_SYNC_AT = "last_sync_at"
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                appwrite.account.get()
            }.onSuccess { user ->
                _authState.value = AuthState(
                    signedIn = true,
                    email = user.email,
                    displayName = user.name.takeIf { it.isNotBlank() },
                    busy = false,
                    error = null
                )
            }.onFailure {
                _authState.value = AuthState(signedIn = false, email = null, busy = false, error = null)
            }
        }
    }
}
