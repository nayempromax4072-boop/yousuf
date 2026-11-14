package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Conversation
import com.example.data.model.NotificationItem
import com.example.data.model.OrderStatus
import com.example.data.model.ServiceCategory
import com.example.data.model.ServiceOrder
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.localization.AppLanguage
import com.example.localization.LocalStrings
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FullScreenImageDialog
import com.example.ui.components.FullScreenVideoDialog
import com.example.ui.components.MetricTile
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElectricYellow
import com.example.ui.theme.ElevatedCharcoal
import com.example.ui.theme.LightGray
import com.example.ui.theme.MutedGray
import com.example.ui.theme.StatusAccepted
import com.example.ui.theme.StatusInProgress
import com.example.ui.theme.StatusPending
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.AdminDashboardStats

@Composable
fun AdminOverviewTab(
    language: AppLanguage,
    stats: AdminDashboardStats,
    recentOrders: List<ServiceOrder>,
    adminNotifications: List<NotificationItem>,
    onOpenOrder: (ServiceOrder) -> Unit,
    onGoToOrders: () -> Unit,
    onGoToChats: () -> Unit,
    onMarkNotificationRead: (String) -> Unit
) {
    val strings = LocalStrings.current

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        item {
            Text(
                text = strings.adminDashboardTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Text(
                text = "YUSUF ELECTRONICS • Verified Admin",
                style = MaterialTheme.typography.bodySmall,
                color = ElectricYellow
            )
        }

        // 7 Statistics Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricTile(
                        title = strings.totalCustomers,
                        value = stats.totalCustomers.toString(),
                        icon = Icons.Filled.Group,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = strings.totalRequests,
                        value = stats.totalRequests.toString(),
                        icon = Icons.Filled.ReceiptLong,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricTile(
                        title = strings.pendingRequests,
                        value = stats.pendingRequests.toString(),
                        icon = Icons.Filled.HourglassTop,
                        accentColor = StatusPending,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = strings.acceptedRequests,
                        value = stats.acceptedRequests.toString(),
                        icon = Icons.Filled.ThumbUp,
                        accentColor = StatusAccepted,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricTile(
                        title = strings.activeJobs,
                        value = stats.activeJobs.toString(),
                        icon = Icons.Filled.Engineering,
                        accentColor = StatusInProgress,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = strings.completedJobs,
                        value = stats.completedJobs.toString(),
                        icon = Icons.Filled.CheckCircle,
                        accentColor = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    border = BorderStroke(1.dp, ElectricYellow.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGoToChats() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.MarkChatUnread, contentDescription = null, tint = ElectricYellow)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(strings.unreadMessages, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        }
                        Badge(containerColor = ElectricYellow, contentColor = DeepBlack) {
                            Text(stats.unreadMessages.toString(), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }

        // Unread Admin Alerts
        val unreadAlerts = adminNotifications.filter { !it.isRead }.take(3)
        if (unreadAlerts.isNotEmpty()) {
            item {
                Text(
                    text = "New Alerts",
                    style = MaterialTheme.typography.titleMedium,
                    color = ElectricYellow,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(unreadAlerts, key = { it.id }) { alert ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ElevatedCharcoal),
                    border = BorderStroke(1.dp, ElectricYellow.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMarkNotificationRead(alert.id) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, tint = ElectricYellow)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(alert.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text(alert.body, style = MaterialTheme.typography.bodySmall, color = LightGray)
                        }
                    }
                }
            }
        }

        // Latest Pending Orders
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Recent Service Requests", style = MaterialTheme.typography.titleMedium, color = Color.White)
                TextButton(onClick = onGoToOrders) {
                    Text(strings.seeAll, color = ElectricYellow)
                }
            }
        }

        if (recentOrders.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Build,
                    title = "No Orders Yet",
                    subtitle = "Incoming customer service bookings will appear here in real time."
                )
            }
        } else {
            items(recentOrders.take(5), key = { it.id }) { order ->
                AdminOrderSummaryCard(order = order, language = language, onClick = { onOpenOrder(order) })
            }
        }
    }
}

@Composable
fun AdminOrdersTab(
    language: AppLanguage,
    ordersState: UiState<List<ServiceOrder>>,
    categories: List<ServiceCategory>,
    onUpdateOrder: (
        order: ServiceOrder,
        newStatus: OrderStatus,
        adminNotes: String,
        scheduledDate: String,
        estimatedCostBdt: Long
    ) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<OrderStatus?>(null) }
    var categoryFilterId by remember { mutableStateOf<String?>(null) }
    var catMenuExpanded by remember { mutableStateOf(false) }
    var selectedOrder by remember { mutableStateOf<ServiceOrder?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Order Management", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by Order ID (YE-...) or Name", color = MutedGray) },
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
                    .testTag("admin_order_search")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                Box {
                    FilterChip(
                        selected = categoryFilterId != null,
                        onClick = { catMenuExpanded = true },
                        label = {
                            val name = categories.firstOrNull { it.id == categoryFilterId }?.localizedName(language) ?: "All Categories"
                            Text(name)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricYellow,
                            selectedLabelColor = DeepBlack,
                            containerColor = CharcoalSurface,
                            labelColor = ElectricYellow
                        )
                    )
                    DropdownMenu(
                        expanded = catMenuExpanded,
                        onDismissRequest = { catMenuExpanded = false },
                        modifier = Modifier.background(CharcoalSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Categories", color = Color.White) },
                            onClick = {
                                categoryFilterId = null
                                catMenuExpanded = false
                            }
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.localizedName(language), color = Color.White) },
                                onClick = {
                                    categoryFilterId = cat.id
                                    catMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text("All Status") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricYellow,
                        selectedLabelColor = DeepBlack,
                        containerColor = CharcoalSurface,
                        labelColor = LightGray
                    )
                )
                OrderStatus.entries.forEach { st ->
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = { statusFilter = st },
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
        }

        when (ordersState) {
            is UiState.Loading -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(color = ElectricYellow)
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.padding(16.dp)) {
                    EmptyStateCard(Icons.Filled.ReceiptLong, "Failed to load orders", ordersState.message)
                }
            }
            is UiState.Success -> {
                val filtered = remember(ordersState.data, searchQuery, statusFilter, categoryFilterId) {
                    ordersState.data.filter { order ->
                        val matchesQuery = searchQuery.isBlank() ||
                            order.orderCode.contains(searchQuery, ignoreCase = true) ||
                            order.customerName.contains(searchQuery, ignoreCase = true) ||
                            order.phone.contains(searchQuery, ignoreCase = true)
                        val matchesStatus = statusFilter == null || order.statusEnum == statusFilter
                        val matchesCat = categoryFilterId == null || order.categoryId == categoryFilterId
                        matchesQuery && matchesStatus && matchesCat
                    }
                }
                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        EmptyStateCard(Icons.Filled.ReceiptLong, "No Matching Orders", "Adjust search or status filters.")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.id }) { order ->
                            AdminOrderSummaryCard(
                                order = order,
                                language = language,
                                onClick = { selectedOrder = order }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedOrder?.let { order ->
        AdminOrderManageDialog(
            order = order,
            language = language,
            onDismiss = { selectedOrder = null },
            onSave = { status, notes, sched, cost ->
                onUpdateOrder(order, status, notes, sched, cost)
                selectedOrder = null
            }
        )
    }
}

@Composable
fun AdminOrderSummaryCard(
    order: ServiceOrder,
    language: AppLanguage,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        border = BorderStroke(1.dp, SubtleBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(order.orderCode, style = MaterialTheme.typography.labelLarge, color = ElectricYellow)
                StatusBadge(status = order.statusEnum, language = language)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(order.categoryName, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text("${order.customerName} • ${order.phone}", style = MaterialTheme.typography.bodySmall, color = LightGray)
            Text(order.address, style = MaterialTheme.typography.bodySmall, color = LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${order.preferredDate} • ${order.preferredTime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricYellow
                )
                Text(
                    text = formatTimestamp(order.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray
                )
            }
        }
    }
}

@Composable
fun AdminOrderManageDialog(
    order: ServiceOrder,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (OrderStatus, String, String, Long) -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current

    var status by remember(order.id) { mutableStateOf(order.statusEnum) }
    var notes by remember(order.id) { mutableStateOf(order.adminNotes) }
    var scheduledDate by remember(order.id) { mutableStateOf(order.scheduledDate) }
    var costText by remember(order.id) { mutableStateOf(if (order.estimatedCostBdt > 0L) order.estimatedCostBdt.toString() else "") }
    var previewImage by remember { mutableStateOf<String?>(null) }
    var previewVideo by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CharcoalSurface,
        title = { Text("${order.orderCode} • ${order.customerName}", color = ElectricYellow) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                Text(order.categoryName, style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text(order.description, style = MaterialTheme.typography.bodySmall, color = LightGray)
                Text("Address: ${order.address}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.phone}")))
                    },
                    border = BorderStroke(1.dp, ElectricYellow)
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, tint = ElectricYellow, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call ${order.phone}", color = ElectricYellow)
                }

                if (order.attachmentUrls.isNotEmpty() || order.videoUrl.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        order.attachmentUrls.forEach { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = "Customer attachment",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { previewImage = url }
                            )
                        }
                        if (order.videoUrl.isNotBlank()) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElevatedCharcoal)
                                    .clickable { previewVideo = order.videoUrl }
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Watch video", tint = ElectricYellow)
                            }
                        }
                    }
                }

                Text("Update Status", style = MaterialTheme.typography.labelLarge, color = Color.White)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    OrderStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.localized(language)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricYellow,
                                selectedLabelColor = DeepBlack,
                                containerColor = DeepBlack,
                                labelColor = LightGray
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = scheduledDate,
                    onValueChange = { scheduledDate = it },
                    label = { Text(strings.scheduledDateLabel) },
                    placeholder = { Text("e.g. 16 Nov, 2:00 PM") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricYellow, unfocusedBorderColor = SubtleBorder)
                )
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(strings.estimatedCostBdtLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricYellow, unfocusedBorderColor = SubtleBorder)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.adminNotesLabel) },
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricYellow, unfocusedBorderColor = SubtleBorder)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costText.toLongOrNull() ?: 0L
                    onSave(status, notes, scheduledDate, cost)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricYellow, contentColor = DeepBlack)
            ) {
                Text(strings.updateOrder, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = LightGray)
            }
        }
    )

    previewImage?.let { FullScreenImageDialog(it) { previewImage = null } }
    previewVideo?.let { FullScreenVideoDialog(it) { previewVideo = null } }
}

@Composable
fun AdminChatsTab(
    conversations: List<Conversation>,
    onOpenConversation: (Conversation) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(16.dp)
    ) {
        Text("Customer Conversations", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))
        if (conversations.isEmpty()) {
            EmptyStateCard(
                icon = Icons.AutoMirrored.Filled.Chat,
                title = "No Active Chats",
                subtitle = "When customers message Yusuf Electronics, threads appear here in real time."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(conversations, key = { it.id }) { conv ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                        border = BorderStroke(
                            1.dp,
                            if (conv.unreadByAdmin > 0) ElectricYellow.copy(alpha = 0.65f) else SubtleBorder
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenConversation(conv) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(ElectricYellow.copy(alpha = 0.16f))
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = ElectricYellow)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(conv.customerName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                    Text(formatTimestamp(conv.updatedAt), style = MaterialTheme.typography.labelSmall, color = MutedGray)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = conv.lastMessageText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LightGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (conv.unreadByAdmin > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Badge(containerColor = ElectricYellow, contentColor = DeepBlack) {
                                    Text(conv.unreadByAdmin.toString())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminCustomersTab(
    customers: List<UserProfile>,
    allOrders: List<ServiceOrder>,
    language: AppLanguage,
    onMessageCustomer: (UserProfile) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<UserProfile?>(null) }

    val filtered = remember(customers, search) {
        customers.filter { it.role != "admin" }.filter {
            search.isBlank() ||
                it.fullName.contains(search, ignoreCase = true) ||
                it.phone.contains(search, ignoreCase = true) ||
                it.email.contains(search, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(16.dp)
    ) {
        Text("Customer Directory", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search customers by name, phone, email", color = MutedGray) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ElectricYellow) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricYellow,
                unfocusedBorderColor = SubtleBorder,
                focusedContainerColor = CharcoalSurface,
                unfocusedContainerColor = CharcoalSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (filtered.isEmpty()) {
            EmptyStateCard(Icons.Filled.Group, "No Customers Found", "Registered customers will appear here.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.uid }) { user ->
                    val custOrders = allOrders.count { it.customerId == user.uid }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                        border = BorderStroke(1.dp, SubtleBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCustomer = user }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            if (user.photoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = user.photoUrl,
                                    contentDescription = user.fullName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, ElectricYellow, CircleShape)
                                )
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(ElevatedCharcoal)
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = ElectricYellow)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.fullName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text(user.phone.ifBlank { user.email }, style = MaterialTheme.typography.bodySmall, color = LightGray)
                            }
                            Surface(
                                color = ElectricYellow.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$custOrders orders",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricYellow,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedCustomer?.let { cust ->
        val custOrders = allOrders.filter { it.customerId == cust.uid }
        AlertDialog(
            onDismissRequest = { selectedCustomer = null },
            containerColor = CharcoalSurface,
            title = { Text(cust.fullName, color = ElectricYellow) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text("Email: ${cust.email}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    Text("Phone: ${cust.phone.ifBlank { "Not set" }}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    Text("Address: ${cust.address.ifBlank { "Not set" }}", color = LightGray, style = MaterialTheme.typography.bodySmall)
                    Text("Joined: ${formatTimestamp(cust.createdAt)}", color = LightGray, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Order History (${custOrders.size})", style = MaterialTheme.typography.titleSmall, color = ElectricYellow)
                    custOrders.take(6).forEach { ord ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${ord.orderCode} • ${ord.categoryName}", style = MaterialTheme.typography.bodySmall, color = Color.White, modifier = Modifier.weight(1f))
                            StatusBadge(status = ord.statusEnum, language = language)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedCustomer = null
                        onMessageCustomer(cust)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricYellow, contentColor = DeepBlack)
                ) {
                    Text("Open Chat", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCustomer = null }) {
                    Text("Close", color = LightGray)
                }
            }
        )
    }
}
