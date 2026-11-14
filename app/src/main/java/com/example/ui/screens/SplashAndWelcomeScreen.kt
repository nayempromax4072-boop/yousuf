package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.firebase.GoogleAuthHelper
import com.example.data.model.UserProfile
import com.example.localization.AppLanguage
import com.example.localization.LocalStrings
import com.example.ui.components.PrimaryYellowButton
import com.example.ui.components.YusufLogoBadge
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElectricYellow
import com.example.ui.theme.ElevatedCharcoal
import com.example.ui.theme.LightGray
import com.example.ui.theme.SubtleBorder
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val strings = LocalStrings.current
    var startAnim by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.78f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "splash_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "splash_alpha"
    )

    val pulse = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        delay(1300L)
        onFinished()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        ElectricYellow.copy(alpha = 0.14f),
                        DeepBlack
                    )
                )
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
                .padding(24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(144.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(2.dp, ElectricYellow.copy(alpha = glowAlpha), RoundedCornerShape(32.dp))
                    .padding(6.dp)
            ) {
                YusufLogoBadge(size = 132.dp)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "YUSUF ELECTRONICS",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = ElectricYellow,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            CircularProgressIndicator(
                color = ElectricYellow,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun WelcomeAuthScreen(
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onAuthSuccess: () -> Unit,
    isResolvingSession: Boolean,
    sessionError: String?,
    onContinueAsGuest: (() -> Unit)? = null
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isSigningIn by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    if (onContinueAsGuest != null) {
        BackHandler { onContinueAsGuest() }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .safeDrawingPadding()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Top language selector bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.ElectricBolt,
                        contentDescription = null,
                        tint = ElectricYellow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BDT (৳) • Bangladesh",
                        style = MaterialTheme.typography.labelMedium,
                        color = LightGray
                    )
                }
                LanguageTogglePill(
                    selected = language,
                    onSelect = onLanguageSelected
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            YusufLogoBadge(size = 108.dp)

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = strings.welcomeTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.welcomeSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = LightGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Highlight feature cards
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                border = BorderStroke(1.dp, SubtleBorder),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    FeatureHighlightRow(
                        icon = Icons.Filled.Build,
                        title = if (language == AppLanguage.BENGALI) "১০+ পেশাদার সার্ভিস ক্যাটাগরি" else "10+ Certified Technical Categories",
                        subtitle = if (language == AppLanguage.BENGALI) "ফ্যান, মোটর, এসি, লাইট, সিসিটিভি, প্লাম্বিং ও বিল্ডিং কাজ" else "Fan, Motor, AC, LED, Wiring, CCTV, Plumbing & Building Maintenance"
                    )
                    FeatureHighlightRow(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        title = if (language == AppLanguage.BENGALI) "রিয়েল-টাইম লাইভ চ্যাট ও মিডিয়া" else "Real-Time Chat with Photo, Video & Voice",
                        subtitle = if (language == AppLanguage.BENGALI) "সমস্যার ছবি, ভিডিও ও ভয়েস মেসেজ সরাসরি অ্যাডমিনকে পাঠান" else "Send problem photos, videos, or voice clips directly to our team"
                    )
                    FeatureHighlightRow(
                        icon = Icons.Filled.VerifiedUser,
                        title = if (language == AppLanguage.BENGALI) "লাইভ অর্ডার ট্র্যাকিং" else "Live Work-Order Status Tracking",
                        subtitle = if (language == AppLanguage.BENGALI) "পেন্ডিং থেকে কাজ সম্পন্ন পর্যন্ত প্রতিটি ধাপের আপডেট" else "Track Pending, Scheduled, In Progress & Completed updates live"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val displayedError = localError ?: sessionError
            if (!displayedError.isNullOrBlank()) {
                Surface(
                    color = DangerRed.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = displayedError,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFCA5A5),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            PrimaryYellowButton(
                text = if (isSigningIn || isResolvingSession) strings.signingIn else strings.signInWithGoogle,
                icon = Icons.AutoMirrored.Filled.Login,
                isLoading = isSigningIn || isResolvingSession,
                testTag = "google_sign_in_button",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    localError = null
                    isSigningIn = true
                    GoogleAuthHelper.onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isSigningIn = false
                            onAuthSuccess()
                        },
                        onAuthError = { err ->
                            isSigningIn = false
                            localError = err
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isSigningIn = false
                        }
                    )
                }
            )

            if (onContinueAsGuest != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onContinueAsGuest,
                    border = BorderStroke(1.dp, SubtleBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("continue_as_guest_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Explore,
                        contentDescription = null,
                        tint = ElectricYellow,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.continueWithoutLogin,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun LanguageTogglePill(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CharcoalSurface,
        border = BorderStroke(1.dp, SubtleBorder),
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = "Language",
                tint = ElectricYellow,
                modifier = Modifier
                    .padding(start = 8.dp, end = 4.dp)
                    .size(16.dp)
            )
            AppLanguage.entries.forEach { lang ->
                val isSelected = lang == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) ElectricYellow else Color.Transparent)
                        .clickable { onSelect(lang) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = lang.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) DeepBlack else LightGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ElectricYellow.copy(alpha = 0.14f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ElectricYellow,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = LightGray
            )
        }
    }
}

@Composable
fun CompleteProfileScreen(
    profile: UserProfile,
    isLoading: Boolean,
    errorMessage: String?,
    onSave: (fullName: String, phone: String, address: String) -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalStrings.current
    var fullName by remember(profile.uid) { mutableStateOf(profile.fullName) }
    var phone by remember(profile.uid) { mutableStateOf(profile.phone) }
    var address by remember(profile.uid) { mutableStateOf(profile.address) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                YusufLogoBadge(size = 48.dp)
                TextButton(onClick = onLogout) {
                    Text(strings.logout, color = LightGray)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = strings.completeProfileTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.completeProfileSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = LightGray
            )
            Spacer(modifier = Modifier.height(20.dp))

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricYellow,
                unfocusedBorderColor = SubtleBorder,
                focusedLabelColor = ElectricYellow,
                unfocusedLabelColor = LightGray,
                cursorColor = ElectricYellow,
                focusedContainerColor = ElevatedCharcoal,
                unfocusedContainerColor = CharcoalSurface
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text(strings.fullNameLabel) },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = ElectricYellow) },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("complete_profile_name")
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = profile.email,
                onValueChange = {},
                enabled = false,
                label = { Text(strings.emailLabel) },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = LightGray) },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(strings.mobileNumberLabel) },
                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = ElectricYellow) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("complete_profile_phone")
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(strings.addressLabel) },
                leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null, tint = ElectricYellow) },
                minLines = 2,
                maxLines = 4,
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("complete_profile_address")
            )

            val shownError = validationError ?: errorMessage
            if (!shownError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = shownError,
                    style = MaterialTheme.typography.bodySmall,
                    color = DangerRed
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            PrimaryYellowButton(
                text = strings.saveAndContinue,
                isLoading = isLoading,
                testTag = "complete_profile_save",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    when {
                        fullName.trim().length < 2 -> validationError = "Please enter your full name."
                        phone.trim().length < 6 -> validationError = "Please enter a valid mobile number."
                        address.trim().length < 4 -> validationError = "Please enter your service address."
                        else -> {
                            validationError = null
                            onSave(fullName, phone, address)
                        }
                    }
                }
            )
        }
    }
}
