package com.example

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.GoogleAuthHelper
import com.example.data.repository.UserRepository
import com.example.localization.AppLanguage
import com.example.localization.BnStrings
import com.example.localization.EnStrings
import com.example.localization.LanguagePreferences
import com.example.localization.LocalStrings
import com.example.service.LocalNotificationHelper
import com.example.ui.screens.AdminMainScreen
import com.example.ui.screens.CompleteProfileScreen
import com.example.ui.screens.CustomerMainScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.WelcomeAuthScreen
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElectricYellow
import com.example.ui.theme.YusufElectronicsTheme
import com.example.ui.viewmodel.AuthViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LocalNotificationHelper.ensureChannel(this)
        setContent {
            YusufElectronicsTheme {
                YusufAppRoot()
            }
        }
    }
}

@Composable
fun YusufAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val authViewModel: AuthViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                AuthViewModel(
                    userRepository = UserRepository(db),
                    languagePreferences = LanguagePreferences(app)
                )
            }
        }
    )

    val language by authViewModel.language.collectAsStateWithLifecycle()
    val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()
    val strings = if (language == AppLanguage.BENGALI) BnStrings else EnStrings

    var showSplash by rememberSaveable { mutableStateOf(true) }
    var showSignInScreen by rememberSaveable { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            GoogleAuthHelper.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    showSignInScreen = false
                },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    LaunchedEffect(currentUser?.uid) {
        val user = currentUser
        if (user == null) {
            authViewModel.onSignedOut()
        } else {
            showSignInScreen = false
            authViewModel.resolveSessionForUser(user)
        }
    }

    // Request POST_NOTIFICATIONS runtime permission on Android 13+ after user signs in
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    CompositionLocalProvider(LocalStrings provides strings) {
        when {
            showSplash -> {
                SplashScreen(onFinished = { showSplash = false })
            }
            currentUser == null && showSignInScreen -> {
                WelcomeAuthScreen(
                    language = language,
                    onLanguageSelected = { authViewModel.setLanguage(it) },
                    onAuthSuccess = {
                        currentUser = Firebase.auth.currentUser
                        showSignInScreen = false
                    },
                    isResolvingSession = sessionState.isResolvingRole,
                    sessionError = sessionState.error,
                    onContinueAsGuest = { showSignInScreen = false }
                )
            }
            currentUser == null -> {
                CustomerMainScreen(
                    currentUserId = "",
                    language = language,
                    onLanguageChange = { authViewModel.setLanguage(it) },
                    onLogout = {},
                    isGuest = true,
                    onRequestSignIn = { showSignInScreen = true }
                )
            }
            else -> {
                val uid = currentUser!!.uid
                key(uid) {
                    val profile = sessionState.profile
                    when {
                        sessionState.isResolvingRole && profile == null -> {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(DeepBlack)
                            ) {
                                CircularProgressIndicator(color = ElectricYellow)
                            }
                        }
                        sessionState.isAdmin -> {
                            AdminMainScreen(
                                currentUserId = uid,
                                language = language,
                                onLanguageChange = { authViewModel.setLanguage(it) },
                                onLogout = {
                                    GoogleAuthHelper.signOut(credentialManager, scope) {
                                        currentUser = null
                                    }
                                }
                            )
                        }
                        profile != null && profile.needsProfileCompletion -> {
                            CompleteProfileScreen(
                                profile = profile,
                                isLoading = sessionState.isResolvingRole,
                                errorMessage = sessionState.error,
                                onSave = { name, phone, addr ->
                                    authViewModel.completeCustomerProfile(uid, name, phone, addr)
                                },
                                onLogout = {
                                    GoogleAuthHelper.signOut(credentialManager, scope) {
                                        currentUser = null
                                    }
                                }
                            )
                        }
                        else -> {
                            CustomerMainScreen(
                                currentUserId = uid,
                                language = language,
                                onLanguageChange = { authViewModel.setLanguage(it) },
                                onLogout = {
                                    GoogleAuthHelper.signOut(credentialManager, scope) {
                                        currentUser = null
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
