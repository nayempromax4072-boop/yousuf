package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.GoogleAuthHelper
import com.example.data.model.UserProfile
import com.example.data.repository.UserRepository
import com.example.localization.AppLanguage
import com.example.localization.LanguagePreferences
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SessionState(
    val isResolvingRole: Boolean = false,
    val isAdmin: Boolean = false,
    val profile: UserProfile? = null,
    val error: String? = null
)

class AuthViewModel(
    private val userRepository: UserRepository,
    private val languagePreferences: LanguagePreferences
) : ViewModel() {

    val language: StateFlow<AppLanguage> = languagePreferences.languageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = AppLanguage.BENGALI
        )

    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            languagePreferences.setLanguage(lang)
        }
    }

    fun onSignedOut() {
        _sessionState.value = SessionState()
    }

    fun resolveSessionForUser(user: FirebaseUser) {
        viewModelScope.launch {
            _sessionState.value = _sessionState.value.copy(isResolvingRole = true, error = null)
            val isAdmin = GoogleAuthHelper.isVerifiedAdmin(user)
            val profileResult = userRepository.ensureUserProfile(
                uid = user.uid,
                email = user.email.orEmpty(),
                displayName = user.displayName.orEmpty(),
                photoUrl = user.photoUrl?.toString().orEmpty(),
                isAdmin = isAdmin
            )
            profileResult.onSuccess { profile ->
                _sessionState.value = SessionState(
                    isResolvingRole = false,
                    isAdmin = isAdmin,
                    profile = profile
                )
                syncFcmToken(user.uid)
            }.onFailure { err ->
                _sessionState.value = SessionState(
                    isResolvingRole = false,
                    isAdmin = isAdmin,
                    profile = null,
                    error = err.localizedMessage ?: "Failed to load user profile."
                )
            }
        }
    }

    fun completeCustomerProfile(
        uid: String,
        fullName: String,
        phone: String,
        address: String
    ) {
        viewModelScope.launch {
            _sessionState.value = _sessionState.value.copy(isResolvingRole = true, error = null)
            val result = userRepository.updateProfile(
                uid = uid,
                fullName = fullName,
                phone = phone,
                address = address
            )
            result.onSuccess {
                val updated = _sessionState.value.profile?.copy(
                    fullName = fullName.trim(),
                    phone = phone.trim(),
                    address = address.trim()
                )
                _sessionState.value = _sessionState.value.copy(
                    isResolvingRole = false,
                    profile = updated
                )
            }.onFailure { err ->
                _sessionState.value = _sessionState.value.copy(
                    isResolvingRole = false,
                    error = err.localizedMessage ?: "Unable to save profile."
                )
            }
        }
    }

    private fun syncFcmToken(uid: String) {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                if (token.isNotBlank()) {
                    userRepository.updateFcmToken(uid, token)
                }
            } catch (e: Exception) {
                Log.d("AuthVM", "FCM token unavailable: ${e.message}")
            }
        }
    }
}
