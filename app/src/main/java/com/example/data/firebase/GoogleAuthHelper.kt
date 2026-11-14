package com.example.data.firebase

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object GoogleAuthHelper {
    private const val TAG = "GoogleAuthHelper"
    const val ADMIN_EMAIL = "nayempromax4072@gmail.com"

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    suspend fun isVerifiedAdmin(user: FirebaseUser?): Boolean {
        if (user == null) return false
        val emailMatch = user.email.equals(ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified
        if (emailMatch) return true
        return try {
            val tokenResult = user.getIdToken(false).await()
            tokenResult.claims["admin"] == true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read ID token claims: ${e.message}")
            false
        }
    }

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope
    ) {
        if (Firebase.auth.currentUser != null) {
            onAuthSuccess()
            return
        }
        val clientId = getDefaultWebClientId(context)
        if (clientId == null) {
            onUnauthenticated()
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val activityContext = context.findActivity() ?: context
                val result = credentialManager.getCredential(activityContext, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    Firebase.auth.signInWithCredential(authCredential).await()
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (e: Exception) {
                Log.d(TAG, "Silent auto sign-in skipped: ${e.message}")
                onUnauthenticated()
            }
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope,
        onAuthCancelled: () -> Unit = {}
    ) {
        val clientId = getDefaultWebClientId(context)
        if (clientId == null) {
            onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val activityContext = context.findActivity() ?: context
                val result = credentialManager.getCredential(activityContext, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    Firebase.auth.signInWithCredential(authCredential).await()
                    onAuthSuccess()
                } else {
                    onAuthError("Unexpected credential type returned.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.errorMessage ?: e.message}", e)
                val detail = e.errorMessage?.toString().orEmpty()
                if (detail.contains("developer", ignoreCase = true) ||
                    detail.contains("28444") ||
                    detail.contains("10:")
                ) {
                    onAuthError("Google Sign-In configuration mismatch (SHA-1): $detail")
                } else {
                    onAuthCancelled()
                }
            } catch (e: NoCredentialException) {
                Log.e(TAG, "No Google credential available", e)
                onAuthError(
                    e.errorMessage?.toString()?.takeIf { it.isNotBlank() }
                        ?: "কোনো Google অ্যাকাউন্ট পাওয়া যায়নি বা অ্যাপের SHA-1 কনফিগারেশন মেলেনি।"
                )
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Credential Manager Sign-In failed: ${e.type}", e)
                onAuthError(
                    e.errorMessage?.toString()?.takeIf { it.isNotBlank() }
                        ?: e.localizedMessage
                        ?: "Google Sign-In failed (${e.type})."
                )
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Google Sign-In failed. Please try again.")
            }
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {}
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    Firebase.auth.signOut()
                    onSignOutComplete()
                }
            }
        }
    }

    private fun getDefaultWebClientId(context: Context): String? {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resId == 0) return null
        return context.getString(resId).takeIf { it.isNotBlank() }
    }
}
