package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Announcement
import com.example.data.model.BusinessSettings
import com.example.data.model.NotificationItem
import com.example.data.model.ServiceCategory
import com.example.data.model.ServiceOrder
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.localization.AppLanguage
import com.example.localization.LocalStrings
import com.example.media.CaptureCompactVideo
import com.example.media.MediaHelper
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MediaSourceChooserDialog
import com.example.ui.components.PrimaryYellowButton
import com.example.ui.components.StatusBadge
import com.example.ui.components.YusufLogoBadge
import com.example.ui.components.categoryIconForKey
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElectricYellow
import com.example.ui.theme.ElevatedCharcoal
import com.example.ui.theme.LightGray
import com.example.ui.theme.MutedGray
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.BookingFormState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CustomerHomeScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    isGuest: Boolean,
    onSignInClick: () -> Unit,
    userProfile: UserProfile?,
    categoriesState: UiState<List<ServiceCategory>>,
    ordersState: UiState<List<ServiceOrder>>,
    notifications: List<NotificationItem>,
    announcements: List<Announcement>,
    businessSettings: BusinessSettings,
    unreadChatCount: Long,
    onOpenNotifications: () -> Unit,
    onSelectCategory: (ServiceCategory) -> Unit,
    onBookServiceClick: () -> Unit,
    onMyOrdersClick: () -> Unit,
    onContactAdminClick: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val unreadNotifCount = notifications.count { !it.isRead }
    val categories = (categoriesState as? UiState.Success)?.data ?: emptyList()
    val filteredCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) categories
        else categories.filter {
            it.nameEn.contains(searchQuery, ignoreCase = true) ||
                it.nameBn.contains(searchQuery, ignoreCase = true) ||
                it.descriptionEn.contains(searchQuery, ignoreCase = true)
        }
    }
    val recentOrders = (ordersState as? UiState.Success)?.data?.take(2) ?: emptyList()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    YusufLogoBadge(size = 46.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        val headerTitle = if (isGuest) {
                            strings.helloUser
                        } else {
                            "${strings.helloUser}, ${userProfile?.fullName?.ifBlank { "Customer" } ?: "Customer"}"
                        }
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (businessSettings.isServiceAvailable) SuccessGreen else DangerRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (businessSettings.isServiceAvailable) strings.serviceOpen else strings.serviceClosed,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (businessSettings.isServiceAvailable) SuccessGreen else DangerRed
                            )
                        }
                    }
                }
                if (isGuest) {
                    LanguageTogglePill(
                        selected = language,
                        onSelect = onLanguageChange
                    )
                } else {
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifCount > 0) {
                                    Badge(containerColor = ElectricYellow, contentColor = DeepBlack) {
                                        Text(unreadNotifCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = strings.recentNotifications,
                                tint = ElectricYellow
                            )
                        }
                    }
                }
            }
        }

        // Hero Booking Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                border = BorderStroke(1.dp, ElectricYellow.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    ElectricYellow.copy(alpha = 0.16f),
                                    CharcoalSurface
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = businessSettings.shopName,
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricYellow
                            )
                            if (isGuest) {
                                Surface(
                                    color = ElectricYellow.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, ElectricYellow.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .clickable { onSignInClick() }
                                        .testTag("home_guest_sign_in_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Login,
                                            contentDescription = strings.signInButtonShort,
                                            tint = ElectricYellow,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = strings.signInButtonShort,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ElectricYellow,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val displayTagline = if (
                            language == AppLanguage.BENGALI &&
                            businessSettings.tagline == "Your Trusted Electrical & Technical Service Partner"
                        ) {
                            strings.tagline
                        } else {
                            businessSettings.tagline
                        }
                        Text(
                            text = displayTagline,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        PrimaryYellowButton(
                            text = strings.bookAService,
                            icon = Icons.Filled.ElectricBolt,
                            testTag = "book_service_cta",
                            onClick = onBookServiceClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onMyOrdersClick,
                                border = BorderStroke(1.dp, SubtleBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("home_my_orders_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = ElectricYellow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.myOrders, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = onContactAdminClick,
                                border = BorderStroke(1.dp, SubtleBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("home_contact_admin_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadChatCount > 0) {
                                            Badge(containerColor = ElectricYellow, contentColor = DeepBlack) {
                                                Text(unreadChatCount.toString())
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        tint = ElectricYellow,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.contactAdmin, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        // Search Services Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchServicesHint, color = MutedGray) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ElectricYellow) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = LightGray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricYellow,
                    unfocusedBorderColor = SubtleBorder,
                    focusedContainerColor = CharcoalSurface,
                    unfocusedContainerColor = CharcoalSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_services")
            )
        }

        // Service Categories Section
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = strings.serviceCategoriesTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = "${filteredCategories.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricYellow
                )
            }
        }

        item {
            if (categoriesState is UiState.Loading && filteredCategories.isEmpty()) {
                LinearProgressIndicator(color = ElectricYellow, modifier = Modifier.fillMaxWidth())
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val columns = if (maxWidth > 620.dp) 3 else 2
                    val rows = filteredCategories.chunked(columns)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        rows.forEach { rowItems ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowItems.forEach { cat ->
                                    ServiceCategoryCard(
                                        category = cat,
                                        language = language,
                                        onClick = { onSelectCategory(cat) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(columns - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Orders Quick Strip
        if (recentOrders.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.myOrders,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    TextButton(onClick = onMyOrdersClick) {
                        Text(strings.seeAll, color = ElectricYellow)
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recentOrders, key = { it.id }) { order ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                            border = BorderStroke(1.dp, SubtleBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .width(260.dp)
                                .clickable { onMyOrdersClick() }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = order.orderCode,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricYellow
                                    )
                                    StatusBadge(status = order.statusEnum, language = language)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = order.categoryName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${order.preferredDate} • ${order.preferredTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LightGray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Announcements / Notifications
        if (announcements.isNotEmpty() || notifications.isNotEmpty()) {
            item {
                Text(
                    text = strings.recentNotifications,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                val latestAnn = announcements.firstOrNull()
                val latestNotif = notifications.firstOrNull()
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    border = BorderStroke(1.dp, SubtleBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenNotifications() }
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        if (latestNotif != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = null,
                                    tint = ElectricYellow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(latestNotif.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                    Text(latestNotif.body, style = MaterialTheme.typography.bodySmall, color = LightGray)
                                }
                            }
                        }
                        if (latestAnn != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.ElectricBolt,
                                    contentDescription = null,
                                    tint = ElectricYellow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(latestAnn.title, style = MaterialTheme.typography.titleSmall, color = ElectricYellow)
                                    Text(latestAnn.body, style = MaterialTheme.typography.bodySmall, color = LightGray)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Business Information Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                border = BorderStroke(1.dp, SubtleBorder),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Store, contentDescription = null, tint = ElectricYellow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.businessInfoTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    Text(
                        text = businessSettings.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = LightGray
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccessTime, contentDescription = null, tint = ElectricYellow, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${strings.workingHoursLabel}: ${businessSettings.workingHours}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = ElectricYellow, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = businessSettings.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${businessSettings.phone}"))
                            context.startActivity(dialIntent)
                        },
                        border = BorderStroke(1.dp, ElectricYellow),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, tint = ElectricYellow, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(businessSettings.phone, color = ElectricYellow, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceCategoryCard(
    category: ServiceCategory,
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        border = BorderStroke(1.dp, SubtleBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElectricYellow.copy(alpha = 0.14f))
            ) {
                Icon(
                    imageVector = categoryIconForKey(category.iconKey),
                    contentDescription = category.localizedName(language),
                    tint = ElectricYellow,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = category.localizedName(language),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )
        }
    }
}

@Composable
fun ServiceCategoryDetailScreen(
    category: ServiceCategory,
    language: AppLanguage,
    onBack: () -> Unit,
    onRequestService: () -> Unit,
    onContactAdmin: () -> Unit
) {
    val strings = LocalStrings.current
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ElectricYellow)
            }
            Text(
                text = category.localizedName(language),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            border = BorderStroke(1.dp, SubtleBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ElectricYellow.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = categoryIconForKey(category.iconKey),
                        contentDescription = null,
                        tint = ElectricYellow,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = category.localizedName(language),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = category.localizedDescription(language),
                    style = MaterialTheme.typography.bodyLarge,
                    color = LightGray
                )
            }
        }
        Spacer(modifier = Modifier.height(22.dp))
        PrimaryYellowButton(
            text = strings.requestService,
            icon = Icons.Filled.ElectricBolt,
            testTag = "category_detail_request_button",
            onClick = onRequestService,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onContactAdmin,
            border = BorderStroke(1.dp, ElectricYellow),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = ElectricYellow)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.contactAdminDirectly, color = ElectricYellow, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ServiceRequestScreen(
    language: AppLanguage,
    isGuest: Boolean,
    onRequestSignIn: () -> Unit,
    userProfile: UserProfile?,
    categories: List<ServiceCategory>,
    preselectedCategory: ServiceCategory?,
    bookingState: BookingFormState,
    onSubmitOrder: (
        customerName: String,
        phone: String,
        categoryId: String,
        categoryName: String,
        description: String,
        address: String,
        preferredDate: String,
        preferredTime: String,
        imageUris: List<Uri>,
        videoUri: Uri?
    ) -> Unit,
    onContactAdminDirectly: () -> Unit,
    onDismissSuccess: () -> Unit,
    onViewMyOrders: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current

    var customerName by remember(userProfile?.uid) { mutableStateOf(userProfile?.fullName ?: "") }
    var phone by remember(userProfile?.uid) { mutableStateOf(userProfile?.phone ?: "") }
    var address by remember(userProfile?.uid) { mutableStateOf(userProfile?.address ?: "") }
    var selectedCategory by remember(preselectedCategory?.id, categories) {
        mutableStateOf(preselectedCategory ?: categories.firstOrNull())
    }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }

    val dateOptions = remember {
        val fmt = SimpleDateFormat("dd MMM yyyy", Locale.US)
        List(4) { offset ->
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }.let { fmt.format(it.time) }
        }
    }
    var preferredDate by remember { mutableStateOf(dateOptions.first()) }
    val timeSlots = listOf("9:00 AM - 12:00 PM", "12:00 PM - 3:00 PM", "3:00 PM - 6:00 PM", "6:00 PM - 9:00 PM")
    var preferredTime by remember { mutableStateOf(timeSlots.first()) }

    val selectedImages = remember { mutableStateListOf<Uri>() }
    var selectedVideo by remember { mutableStateOf<Uri?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var showVideoSourceDialog by remember { mutableStateOf(false) }
    var pendingCameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraVideoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(3)
    ) { uris ->
        selectedImages.clear()
        selectedImages.addAll(uris.take(3))
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) selectedVideo = uri
    }

    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraPhotoUri?.let { uri ->
                if (selectedImages.size < 3) {
                    selectedImages.add(uri)
                } else {
                    selectedImages[2] = uri
                }
            }
        }
    }

    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = CaptureCompactVideo()
    ) { success ->
        if (success) {
            pendingCameraVideoUri?.let { selectedVideo = it }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ElectricYellow,
        unfocusedBorderColor = SubtleBorder,
        focusedLabelColor = ElectricYellow,
        unfocusedLabelColor = LightGray,
        focusedContainerColor = ElevatedCharcoal,
        unfocusedContainerColor = CharcoalSurface,
        cursorColor = ElectricYellow
    )

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = strings.bookAService,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            if (isGuest) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = ElectricYellow.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, ElectricYellow.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = strings.signInRequiredForBooking,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onRequestSignIn) {
                            Text(strings.signInButtonShort, color = ElectricYellow, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            // Category Dropdown Selector
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedCategory?.localizedName(language) ?: "Select Category",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(strings.serviceCategoriesTitle) },
                    leadingIcon = {
                        Icon(
                            imageVector = categoryIconForKey(selectedCategory?.iconKey ?: "other"),
                            contentDescription = null,
                            tint = ElectricYellow
                        )
                    },
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { categoryDropdownExpanded = true }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { categoryDropdownExpanded = true }
                )
                DropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false },
                    modifier = Modifier.background(CharcoalSurface)
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.localizedName(language), color = Color.White) },
                            leadingIcon = {
                                Icon(
                                    imageVector = categoryIconForKey(cat.iconKey),
                                    contentDescription = null,
                                    tint = ElectricYellow
                                )
                            },
                            onClick = {
                                selectedCategory = cat
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text(strings.fullNameLabel) },
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(strings.addressLabel) },
                minLines = 2,
                maxLines = 3,
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(strings.problemDescriptionLabel) },
                minLines = 3,
                maxLines = 5,
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("order_description_input")
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text(strings.preferredDateLabel, style = MaterialTheme.typography.titleSmall, color = Color.White)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                dateOptions.forEach { d ->
                    FilterChip(
                        selected = preferredDate == d,
                        onClick = { preferredDate = d },
                        label = { Text(d) },
                        leadingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricYellow,
                            selectedLabelColor = DeepBlack,
                            selectedLeadingIconColor = DeepBlack,
                            containerColor = CharcoalSurface,
                            labelColor = LightGray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(strings.preferredTimeLabel, style = MaterialTheme.typography.titleSmall, color = Color.White)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                timeSlots.forEach { slot ->
                    FilterChip(
                        selected = preferredTime == slot,
                        onClick = { preferredTime = slot },
                        label = { Text(slot) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricYellow,
                            selectedLabelColor = DeepBlack,
                            containerColor = CharcoalSurface,
                            labelColor = LightGray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { showPhotoSourceDialog = true },
                    border = BorderStroke(1.dp, SubtleBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = ElectricYellow)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${strings.addPhotos} (${selectedImages.size})", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = { showVideoSourceDialog = true },
                    border = BorderStroke(1.dp, SubtleBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Filled.VideoFile, contentDescription = null, tint = ElectricYellow)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (selectedVideo != null) "Video (1)" else strings.addVideo,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (selectedImages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedImages.forEach { uri ->
                        Box {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected attachment",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            IconButton(
                                onClick = { selectedImages.remove(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(DeepBlack.copy(alpha = 0.8f))
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            if (selectedVideo != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = CharcoalSurface,
                    border = BorderStroke(1.dp, SubtleBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Filled.VideoFile, contentDescription = null, tint = ElectricYellow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Video attached", color = Color.White, modifier = Modifier.weight(1f))
                        IconButton(onClick = { selectedVideo = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove video", tint = LightGray)
                        }
                    }
                }
            }

            if (bookingState.isSubmitting) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { bookingState.uploadProgress },
                    color = ElectricYellow,
                    trackColor = SubtleBorder,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val err = validationError ?: bookingState.errorMessage
            if (!err.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(err, style = MaterialTheme.typography.bodySmall, color = DangerRed)
            }

            Spacer(modifier = Modifier.height(18.dp))
            PrimaryYellowButton(
                text = if (isGuest) strings.signInWithGoogle else strings.submitServiceRequest,
                isLoading = bookingState.isSubmitting,
                testTag = "submit_order_button",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (isGuest) {
                        onRequestSignIn()
                        return@PrimaryYellowButton
                    }
                    val cat = selectedCategory
                    when {
                        cat == null -> validationError = "Please select a service category."
                        customerName.trim().length < 2 -> validationError = "Please enter your name."
                        phone.trim().length < 5 -> validationError = "Please enter your mobile number."
                        address.trim().length < 4 -> validationError = "Please enter your full address."
                        description.trim().length < 4 -> validationError = "Please describe the problem."
                        else -> {
                            validationError = null
                            onSubmitOrder(
                                customerName,
                                phone,
                                cat.id,
                                cat.localizedName(language),
                                description,
                                address,
                                preferredDate,
                                preferredTime,
                                selectedImages.toList(),
                                selectedVideo
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onContactAdminDirectly,
                border = BorderStroke(1.dp, ElectricYellow),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = ElectricYellow)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.contactAdminDirectly, color = ElectricYellow, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showPhotoSourceDialog) {
        MediaSourceChooserDialog(
            title = strings.choosePhotoSource,
            cameraLabel = strings.takePhotoCamera,
            galleryLabel = strings.choosePhotoGallery,
            cancelLabel = strings.cancel,
            isVideo = false,
            onCameraClick = {
                try {
                    val uri = MediaHelper.createCameraTempUri(context, "order_photo", "jpg")
                    pendingCameraPhotoUri = uri
                    takePhotoLauncher.launch(uri)
                } catch (_: ActivityNotFoundException) {
                    validationError = "Camera app unavailable on this device."
                } catch (e: Exception) {
                    validationError = e.localizedMessage ?: "Unable to open camera."
                }
            },
            onGalleryClick = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDismiss = { showPhotoSourceDialog = false }
        )
    }

    if (showVideoSourceDialog) {
        MediaSourceChooserDialog(
            title = strings.chooseVideoSource,
            cameraLabel = strings.recordVideoCamera,
            galleryLabel = strings.chooseVideoGallery,
            cancelLabel = strings.cancel,
            isVideo = true,
            onCameraClick = {
                try {
                    val uri = MediaHelper.createCameraTempUri(context, "order_video", "mp4")
                    pendingCameraVideoUri = uri
                    captureVideoLauncher.launch(uri)
                } catch (_: ActivityNotFoundException) {
                    validationError = "Video camera unavailable on this device."
                } catch (e: Exception) {
                    validationError = e.localizedMessage ?: "Unable to open camera video."
                }
            },
            onGalleryClick = {
                videoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                )
            },
            onDismiss = { showVideoSourceDialog = false }
        )
    }

    if (bookingState.submittedOrderCode != null) {
        AlertDialog(
            onDismissRequest = onDismissSuccess,
            containerColor = CharcoalSurface,
            icon = {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = { Text(strings.orderSubmittedTitle, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.orderSubmittedMessage, color = LightGray)
                    Surface(
                        color = ElectricYellow.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${strings.orderIdLabel}: ${bookingState.submittedOrderCode}",
                            style = MaterialTheme.typography.titleSmall,
                            color = ElectricYellow,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        onDismissSuccess()
                        onViewMyOrders()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricYellow, contentColor = DeepBlack)
                ) {
                    Text(strings.myOrders, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSuccess) {
                    Text("OK", color = LightGray)
                }
            }
        )
    }
}
