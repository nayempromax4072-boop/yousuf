package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Announcement
import com.example.data.model.NotificationItem
import com.example.data.model.OrderStatus
import com.example.data.model.ServiceOrder
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.localization.AppLanguage
import com.example.localization.LocalStrings
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FullScreenImageDialog
import com.example.ui.components.FullScreenVideoDialog
import com.example.ui.components.MetricTile
import com.example.ui.components.PrimaryYellowButton
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatTimestamp
import com.example.ui.components.orderStatusColor
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElectricYellow
import com.example.ui.theme.ElevatedCharcoal
import com.example.ui.theme.LightGray
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.SuccessGreen

@Composable
fun CustomerOrdersScreen(
    language: AppLanguage,
    isGuest: Boolean,
    onSignInClick: () -> Unit,
    ordersState: UiState<List<ServiceOrder>>,
    onCancelPendingOrder: (String) -> Unit,
    onBookServiceClick: () -> Unit
) {
    val strings = LocalStrings.current
    var selectedFilter by remember { mutableStateOf<OrderStatus?>(null) }
    var previewImage by remember { mutableStateOf<String?>(null) }
    var previewVideo by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Text(
            text = strings.myOrders,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text(if (language == AppLanguage.BENGALI) "সব" else "All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricYellow,
                    selectedLabelColor = DeepBlack,
                    containerColor = CharcoalSurface,
                    labelColor = LightGray
                )
            )
            OrderStatus.entries.forEach { st ->
                FilterChip(
                    selected = selectedFilter == st,
                    onClick = { selectedFilter = st },
                    label = { Text(st.localized(language)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricYellow,
                        selectedLabelColor = DeepBlack,
                        containerColor = CharcoalSurface,
                        labelColor = LightGray
                    )
                )
            }
        }

        if (isGuest) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                EmptyStateCard(
                    icon = Icons.Filled.ReceiptLong,
                    title = strings.myOrders,
                    subtitle = strings.signInRequiredForOrders
                )
                Spacer(modifier = Modifier.height(14.dp))
                PrimaryYellowButton(
                    text = strings.signInWithGoogle,
                    icon = Icons.AutoMirrored.Filled.Login,
                    onClick = onSignInClick,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onBookServiceClick,
                    border = BorderStroke(1.dp, SubtleBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(strings.bookAService, color = Color.White)
                }
            }
        } else {
            when (ordersState) {
                is UiState.Loading -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(color = ElectricYellow)
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        EmptyStateCard(
                            icon = Icons.Filled.ReceiptLong,
                            title = "Error Loading Orders",
                            subtitle = ordersState.message
                        )
                    }
                }
                is UiState.Success -> {
                    val filtered = remember(ordersState.data, selectedFilter) {
                        if (selectedFilter == null) ordersState.data
                        else ordersState.data.filter { it.statusEnum == selectedFilter }
                    }

                    if (filtered.isEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        ) {
                            EmptyStateCard(
                                icon = Icons.Filled.ReceiptLong,
                                title = strings.noOrdersYet,
                                subtitle = strings.noOrdersHint
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            PrimaryYellowButton(
                                text = strings.bookAService,
                                onClick = onBookServiceClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.id }) { order ->
                            CustomerOrderCard(
                                order = order,
                                language = language,
                                cancelLabel = strings.cancelOrder,
                                onCancel = { onCancelPendingOrder(order.id) },
                                onOpenImage = { previewImage = it },
                                onOpenVideo = { previewVideo = it }
                            )
                        }
                        }
                    }
                }
            }
        }
    }

    previewImage?.let { FullScreenImageDialog(it) { previewImage = null } }
    previewVideo?.let { FullScreenVideoDialog(it) { previewVideo = null } }
}

@Composable
private fun CustomerOrderCard(
    order: ServiceOrder,
    language: AppLanguage,
    cancelLabel: String,
    onCancel: () -> Unit,
    onOpenImage: (String) -> Unit,
    onOpenVideo: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        border = BorderStroke(1.dp, SubtleBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = order.orderCode,
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricYellow
                )
                StatusBadge(status = order.statusEnum, language = language)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = order.categoryName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = order.description,
                style = MaterialTheme.typography.bodyMedium,
                color = LightGray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Schedule: ${order.preferredDate} • ${order.preferredTime}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            if (order.scheduledDate.isNotBlank()) {
                Text(
                    text = "Confirmed Appointment: ${order.scheduledDate}",
                    style = MaterialTheme.typography.labelMedium,
                    color = SuccessGreen
                )
            }
            if (order.estimatedCostBdt > 0L) {
                Text(
                    text = "Estimated / Billed: ৳${order.estimatedCostBdt}",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricYellow
                )
            }
            if (order.adminNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = ElevatedCharcoal,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Technician Note: ${order.adminNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (order.attachmentUrls.isNotEmpty() || order.videoUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    order.attachmentUrls.forEach { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Order photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenImage(url) }
                        )
                    }
                    if (order.videoUrl.isNotBlank()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElevatedCharcoal)
                                .border(1.dp, ElectricYellow.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable { onOpenVideo(order.videoUrl) }
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Watch video", tint = ElectricYellow)
                        }
                    }
                }
            }

            // Progress bar indicator for active lifecycle
            if (order.statusEnum != OrderStatus.CANCELLED) {
                Spacer(modifier = Modifier.height(12.dp))
                val stepIndex = when (order.statusEnum) {
                    OrderStatus.PENDING -> 1
                    OrderStatus.ACCEPTED -> 2
                    OrderStatus.CONTACTED -> 3
                    OrderStatus.WORK_SCHEDULED -> 4
                    OrderStatus.IN_PROGRESS -> 5
                    OrderStatus.COMPLETED -> 6
                    OrderStatus.CANCELLED -> 0
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in 1..6) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i <= stepIndex) orderStatusColor(order.statusEnum)
                                    else SubtleBorder
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formatTimestamp(order.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = LightGray
                )
                if (order.statusEnum == OrderStatus.PENDING) {
                    TextButton(onClick = onCancel) {
                        Text(cancelLabel, color = DangerRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerProfileScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    isGuest: Boolean,
    onSignInClick: () -> Unit,
    userProfile: UserProfile?,
    orders: List<ServiceOrder>,
    onUpdateProfile: (fullName: String, phone: String, address: String, photoUri: Uri?) -> Unit,
    onNavigateOrders: () -> Unit,
    onNavigateChat: () -> Unit,
    onOpenNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalStrings.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val totalCount = orders.size
    val completedCount = orders.count { it.statusEnum == OrderStatus.COMPLETED }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && userProfile != null) {
            onUpdateProfile(userProfile.fullName, userProfile.phone, userProfile.address, uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = strings.profileTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            border = BorderStroke(1.dp, SubtleBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                if (isGuest) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(ElevatedCharcoal)
                            .border(2.dp, ElectricYellow, CircleShape)
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = ElectricYellow, modifier = Modifier.size(40.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = strings.guestUserLabel,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.signInToUnlockFull,
                        style = MaterialTheme.typography.bodySmall,
                        color = LightGray
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    PrimaryYellowButton(
                        text = strings.signInWithGoogle,
                        icon = Icons.AutoMirrored.Filled.Login,
                        onClick = onSignInClick,
                        testTag = "profile_guest_sign_in_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier.clickable {
                            avatarPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        if (!userProfile?.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userProfile?.photoUrl,
                                contentDescription = "Profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, ElectricYellow, CircleShape)
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(CircleShape)
                                    .background(ElevatedCharcoal)
                                    .border(2.dp, ElectricYellow, CircleShape)
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = ElectricYellow, modifier = Modifier.size(40.dp))
                            }
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ElectricYellow)
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = "Change Photo", tint = DeepBlack, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = userProfile?.fullName ?: "Customer",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = userProfile?.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightGray
                    )
                    if (!userProfile?.phone.isNullOrBlank()) {
                        Text(
                            text = userProfile?.phone.orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricYellow,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (!userProfile?.address.isNullOrBlank()) {
                        Text(
                            text = userProfile?.address.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = LightGray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${strings.memberSinceLabel}: ${formatTimestamp(userProfile?.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LightGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showEditDialog = true },
                        border = BorderStroke(1.dp, ElectricYellow),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = ElectricYellow, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.editProfile, color = ElectricYellow)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (!isGuest) {

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MetricTile(
                title = strings.totalOrdersLabel,
                value = totalCount.toString(),
                icon = Icons.Filled.ReceiptLong,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                title = strings.completedOrdersLabel,
                value = completedCount.toString(),
                icon = Icons.Filled.CheckCircle,
                accentColor = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(strings.chooseLanguage, style = MaterialTheme.typography.titleSmall, color = Color.White)
            LanguageTogglePill(selected = language, onSelect = onLanguageChange)
        }

        Spacer(modifier = Modifier.height(10.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            border = BorderStroke(1.dp, SubtleBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                ProfileMenuItem(Icons.Filled.ReceiptLong, strings.myOrders, onNavigateOrders)
                HorizontalDivider(color = SubtleBorder)
                ProfileMenuItem(Icons.AutoMirrored.Filled.Chat, strings.chatWithAdmin, onNavigateChat)
                HorizontalDivider(color = SubtleBorder)
                ProfileMenuItem(Icons.Filled.Notifications, strings.recentNotifications, onOpenNotifications)
                HorizontalDivider(color = SubtleBorder)
                ProfileMenuItem(Icons.Filled.PrivacyTip, strings.privacyPolicy) { showPrivacyDialog = true }
                HorizontalDivider(color = SubtleBorder)
                ProfileMenuItem(Icons.Filled.Info, strings.aboutUs) { showAboutDialog = true }
            }
        }

        if (!isGuest) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onLogout,
                border = BorderStroke(1.dp, DangerRed),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = DangerRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.logout, color = DangerRed, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showEditDialog && userProfile != null) {
        var name by remember { mutableStateOf(userProfile.fullName) }
        var phone by remember { mutableStateOf(userProfile.phone) }
        var address by remember { mutableStateOf(userProfile.address) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = CharcoalSurface,
            title = { Text(strings.editProfile, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(strings.fullNameLabel) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricYellow, unfocusedBorderColor = SubtleBorder)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(strings.mobileNumberLabel) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricYellow, unfocusedBorderColor = SubtleBorder)
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(strings.addressLabel) },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricYellow, unfocusedBorderColor = SubtleBorder)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(name, phone, address, null)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricYellow, contentColor = DeepBlack)
                ) {
                    Text(strings.saveChanges, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(strings.cancel, color = LightGray)
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = CharcoalSurface,
            title = { Text(strings.privacyPolicy, color = Color.White) },
            text = {
                Text(
                    "Yusuf Electronics collects only your name, email, mobile number, and service address to schedule technical work orders and support messages. Your private chats and orders are protected by Firebase security rules and are accessible only to you and verified Yusuf Electronics administrators.",
                    color = LightGray,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("OK", color = ElectricYellow)
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = CharcoalSurface,
            title = { Text("YUSUF ELECTRONICS", color = ElectricYellow) },
            text = {
                Text(
                    "Your Trusted Electrical & Technical Service Partner in Bangladesh. Providing professional fan, electric motor, AC, LED lighting, wiring, CCTV, water pipeline plumbing, and complete building maintenance services.",
                    color = LightGray,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK", color = ElectricYellow)
                }
            }
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = ElectricYellow, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Color.White)
    }
}

@Composable
fun NotificationsDialog(
    notifications: List<NotificationItem>,
    announcements: List<Announcement>,
    onMarkRead: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CharcoalSurface,
        title = { Text(strings.recentNotifications, color = Color.White) },
        text = {
            if (notifications.isEmpty() && announcements.isEmpty()) {
                Text(strings.noNotificationsYet, color = LightGray)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(340.dp)
                ) {
                    items(announcements, key = { "ann_${it.id}" }) { ann ->
                        Surface(
                            color = ElectricYellow.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, ElectricYellow.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(ann.title, style = MaterialTheme.typography.titleSmall, color = ElectricYellow)
                                Text(ann.body, style = MaterialTheme.typography.bodySmall, color = Color.White)
                                Text(formatTimestamp(ann.createdAt), style = MaterialTheme.typography.labelSmall, color = LightGray)
                            }
                        }
                    }
                    items(notifications, key = { it.id }) { notif ->
                        Surface(
                            color = if (!notif.isRead) ElevatedCharcoal else DeepBlack,
                            border = BorderStroke(1.dp, SubtleBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (!notif.isRead) onMarkRead(notif.id) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(notif.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                    if (!notif.isRead) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(ElectricYellow)
                                        )
                                    }
                                }
                                Text(notif.body, style = MaterialTheme.typography.bodySmall, color = LightGray)
                                Text(formatTimestamp(notif.createdAt), style = MaterialTheme.typography.labelSmall, color = LightGray)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ElectricYellow)
            }
        }
    )
}
