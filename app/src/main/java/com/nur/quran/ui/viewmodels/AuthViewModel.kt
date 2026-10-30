package com.nur.quran.ui.viewmodels

import com.nur.quran.R

import android.content.Context
import com.nur.quran.data.auth.GoogleSignIn
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nur.quran.data.sync.SyncService
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val message: String? = null,
    val googleAvailable: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val rnApiClient: com.nur.quran.data.api.rn.RnApiClient,
    private val syncService: SyncService,
    private val syncRealtime: com.nur.quran.data.sync.SyncRealtime,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        refresh()
        loadGoogleAvailability()
    }

    private fun loadGoogleAvailability() {
        viewModelScope.launch(Dispatchers.IO) {
            val enabled = try { rnApiClient.googleProviders().google.enabled }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { false }
            _authState.value = _authState.value.copy(googleAvailable = enabled)
        }
    }

    /** Runs on Main so Credential Manager receives the Activity's UI context. */
    fun continueWithGoogle(activityContext: Context) {
        if (_authState.value.busy) return
        viewModelScope.launch {
            _authState.value = _authState.value.copy(busy = true, error = null, message = null)
            try {
                val challenge = withContext(Dispatchers.IO) { rnApiClient.googleChallenge() }
                val token = GoogleSignIn.idToken(activityContext, challenge.clientId, challenge.nonce)
                val user = withContext(Dispatchers.IO) { rnApiClient.signInWithGoogle(token, challenge.nonce) }
                    ?: error("Incomplete authentication response")
                resetSyncWatermark()
                _authState.value = AuthState(signedIn = true, email = user.email, displayName = user.name,
                    googleAvailable = true)
                restoreFromCloud()
            } catch (_: GetCredentialCancellationException) {
                _authState.value = _authState.value.copy(busy = false)
            } catch (_: NoCredentialException) {
                _authState.value = _authState.value.copy(busy = false,
                    error = com.nur.quran.i18n.AppLanguages.wrap(context).getString(R.string.ui_add_a_google_account_on_this_device_then_try_aga_761232))
            } catch (cancelled: CancellationException) {
                _authState.value = _authState.value.copy(busy = false)
                throw cancelled
            } catch (error: Exception) {
                _authState.value = _authState.value.copy(busy = false,
                    error = if (error is retrofit2.HttpException) authErrorMessage(error, "Google sign-in is unavailable. Please try again.")
                        else "Google sign-in is unavailable. Please try again.")
            }
        }
    }

    fun isPasswordValid(pw: String) = pw.length >= 8

    fun login(email: String, password: String) {
        if (!isPasswordValid(password)) {
            _authState.value = _authState.value.copy(
                busy = false,
                error = com.nur.quran.i18n.AppLanguages.wrap(context).getString(R.string.ui_password_must_be_at_least_8_characters_3e7d56),
                message = null
            )
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _authState.value = _authState.value.copy(busy = true, error = null, message = null)
            runCatching {
                val user = rnApiClient.login(email, password) ?: error("Login failed")
                Pair(user.email ?: email.trim(), user.name)
            }.onSuccess { (mail, displayName) ->
                resetSyncWatermark()
                _authState.value = AuthState(
                    signedIn = true,
                    email = mail,
                    displayName = displayName,
                    busy = false,
                    error = null,
                    googleAvailable = _authState.value.googleAvailable
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
        rnApiClient.storedUserId()?.let { syncRealtime.start(it) }
        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching { syncService.pull() }.getOrDefault(false)
            android.util.Log.d("NurSync", "restoreFromCloud after login: pull ok=$ok")
        }
    }

    fun register(email: String, password: String, name: String = "") {
        if (!isPasswordValid(password)) {
            _authState.value = _authState.value.copy(
                busy = false,
                error = com.nur.quran.i18n.AppLanguages.wrap(context).getString(R.string.ui_password_must_be_at_least_8_characters_3e7d56),
                message = null
            )
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _authState.value = _authState.value.copy(busy = true, error = null, message = null)
            runCatching {
                // Registration establishes the session in one backend request.
                val user = rnApiClient.register(email.trim(), password, name.trim())
                    ?: error("Registration failed")
                Pair(user.email ?: email.trim(), user.name)
            }.onSuccess { (mail, displayName) ->
                resetSyncWatermark()
                _authState.value = AuthState(
                    signedIn = true,
                    email = mail,
                    displayName = displayName,
                    busy = false,
                    error = null,
                    googleAvailable = _authState.value.googleAvailable
                )
                restoreFromCloud()
            }.onFailure { e ->
                _authState.value = _authState.value.copy(busy = false, error = authErrorMessage(e, "Registration failed"))
            }
        }
    }

    fun sendRecovery(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _authState.value = _authState.value.copy(busy = true, error = null, message = null)
            runCatching {
                rnApiClient.requestRecovery(email)
            }.onSuccess {
                _authState.value = _authState.value.copy(
                    busy = false,
                    error = null,
                    message = com.nur.quran.i18n.AppLanguages.wrap(context).getString(R.string.ui_if_an_account_exists_for_this_email_a_password_r_3813b2)
                )
            }.onFailure { e ->
                _authState.value = _authState.value.copy(
                    busy = false,
                    error = authErrorMessage(e, "Recovery failed")
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
                syncRealtime.stop()
                rnApiClient.clearSession()
                try { GoogleSignIn.clear(context) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* Local tokens are already cleared. */ }
            }.onSuccess {
                resetSyncWatermark()
                _authState.value = AuthState(signedIn = false, email = null, busy = false, error = null, googleAvailable = _authState.value.googleAvailable)
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
            _authState.value = AuthState(
                signedIn = rnApiClient.hasSession(),
                email = rnApiClient.storedUserEmail().takeIf { rnApiClient.hasSession() },
                busy = false,
                googleAvailable = _authState.value.googleAvailable
            )
        }
    }
}

internal fun authErrorMessage(error: Throwable, fallback: String): String {
    if (error is retrofit2.HttpException) {
        val body = runCatching { com.google.gson.JsonParser.parseString(error.response()?.errorBody()?.string()).asJsonObject }.getOrNull()
        return when (body?.get("error")?.asString) {
            "google_not_configured" -> "Google sign-in is temporarily unavailable."
            "invalid_google_token", "invalid_google_challenge" -> "Google sign-in expired. Please choose your account again."
            "google_email_verification_required" -> "Google cannot confirm ownership of this email. Sign in with email or use Forgot password."
            "google_account_conflict" -> "This email is linked to a different Google account. Use your original sign-in method."
            "google_sign_in_busy" -> "Google sign-in is busy. Please try again shortly."
            "account_requires_recovery" -> "This account already exists. Use Forgot password to verify your email and set your password."
            "recovery_not_configured" -> "Password recovery is temporarily unavailable. Please try again later."
            "email_exists" -> "An account already exists for this email. Sign in or use Forgot password."
            "invalid_input" -> "Check your email and password and try again."
            else -> error.message ?: fallback
        }
    }
    return error.message ?: fallback
}
