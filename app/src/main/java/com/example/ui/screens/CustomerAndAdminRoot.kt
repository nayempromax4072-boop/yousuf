package com.example.ui.screens

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.Conversation
import com.example.data.model.ServiceCategory
import com.example.data.model.UiState
import com.example.data.repository.CategoryRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.UserRepository
import com.example.localization.AppLanguage
import com.example.localization.LocalStrings
import com.example.service.LocalNotificationHelper
import com.example.ui.components.YusufLogoBadge
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElectricYellow
import com.example.ui.theme.LightGray
import com.example.ui.theme.SubtleBorder
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.CustomerViewModel
import com.google.firebase.firestore.FirebaseFirestore

private data class NavTabItem(
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun CustomerMainScreen(
    currentUserId: String,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    isGuest: Boolean = false,
    onRequestSignIn: () -> Unit = {}
) {
    val strings = LocalStrings.current
    val context = LocalContext.current

    val customerViewModel: CustomerViewModel = viewModel(
        key = "customer_${currentUserId.ifBlank { "guest" }}",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                CustomerViewModel(
                    userRepository = UserRepository(db),
                    categoryRepository = CategoryRepository(db),
                    orderRepository = OrderRepository(db),
                    notificationRepository = NotificationRepository(db),
                    settingsRepository = SettingsRepository(db),
                    chatRepository = ChatRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val chatViewModel: ChatViewModel? = if (isGuest) {
        null
    } else {
        viewModel(
            key = "customer_chat_$currentUserId",
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) as Application
                    val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                    ChatViewModel(
                        chatRepository = ChatRepository(db),
                        conversationId = currentUserId,
                        senderRole = "customer"
                    )
                }
            }
        )
    }

    val userProfile by customerViewModel.userProfile.collectAsStateWithLifecycle()
    val categoriesState by customerViewModel.categories.collectAsStateWithLifecycle()
    val ordersState by customerViewModel.orders.collectAsStateWithLifecycle()
    val notifications by customerViewModel.notifications.collectAsStateWithLifecycle()
    val announcements by customerViewModel.announcements.collectAsStateWithLifecycle()
    val businessSettings by customerViewModel.businessSettings.collectAsStateWithLifecycle()
    val conversation by customerViewModel.conversation.collectAsStateWithLifecycle()
    val bookingState by customerViewModel.bookingState.collectAsStateWithLifecycle()
    val profileMsg by customerViewModel.profileMessage.collectAsStateWithLifecycle()

    // Trigger local Android system alert when a new unread notification arrives
    val seenNotifIds = remember { mutableSetOf<String>() }
    LaunchedEffect(notifications, isGuest) {
        if (!isGuest) {
            val newest = notifications.firstOrNull { !it.isRead }
            if (newest != null && seenNotifIds.add(newest.id)) {
                LocalNotificationHelper.showNotification(
                    context = context,
                    id = newest.id.hashCode(),
                    title = newest.title,
                    body = newest.body
                )
            }
        }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var activeCategoryDetail by remember { mutableStateOf<ServiceCategory?>(null) }
    var preselectedBookingCategory by remember { mutableStateOf<ServiceCategory?>(null) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedTab != 0 && activeCategoryDetail == null) {
        selectedTab = 0
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(profileMsg) {
        if (profileMsg != null) {
            snackbarHostState.showSnackbar(profileMsg!!)
            customerViewModel.clearProfileMessage()
        }
    }

    val unreadChat = (conversation?.unreadByCustomer ?: 0L).toInt()
    val navItems = listOf(
        NavTabItem(strings.navHome, Icons.Filled.Home),
        NavTabItem(strings.navServices, Icons.Filled.Build),
        NavTabItem(strings.navOrders, Icons.Filled.ReceiptLong),
        NavTabItem(strings.navChat, Icons.AutoMirrored.Filled.Chat, badgeCount = unreadChat),
        NavTabItem(strings.navProfile, Icons.Filled.Person)
    )

    val categoriesList = (categoriesState as? UiState.Success)?.data ?: emptyList()
    val ordersList = (ordersState as? UiState.Success)?.data ?: emptyList()

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        val isWide = maxWidth >= 700.dp

        Scaffold(
            containerColor = DeepBlack,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                if (!isWide && activeCategoryDetail == null) {
                    NavigationBar(
                        containerColor = CharcoalSurface,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        navItems.forEachIndexed { idx, item ->
                            NavigationBarItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (item.badgeCount > 0) {
                                                Badge(containerColor = ElectricYellow, contentColor = DeepBlack) {
                                                    Text(item.badgeCount.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(item.icon, contentDescription = item.label)
                                    }
                                },
                                label = {
                                    Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DeepBlack,
                                    selectedTextColor = ElectricYellow,
                                    indicatorColor = ElectricYellow,
                                    unselectedIconColor = LightGray,
                                    unselectedTextColor = LightGray
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWide) {
                    NavigationRail(
                        containerColor = CharcoalSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.size(12.dp))
                        YusufLogoBadge(size = 40.dp)
                        Spacer(modifier = Modifier.size(16.dp))
                        navItems.forEachIndexed { idx, item ->
                            NavigationRailItem(
                                selected = selectedTab == idx && activeCategoryDetail == null,
                                onClick = {
                                    activeCategoryDetail = null
                                    selectedTab = idx
                                },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (item.badgeCount > 0) {
                                                Badge(containerColor = ElectricYellow, contentColor = DeepBlack) {
                                                    Text(item.badgeCount.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(item.icon, contentDescription = item.label)
                                    }
                                },
                                label = { Text(item.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = DeepBlack,
                                    selectedTextColor = ElectricYellow,
                                    indicatorColor = ElectricYellow,
                                    unselectedIconColor = LightGray,
                                    unselectedTextColor = LightGray
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    val detailCat = activeCategoryDetail
                    if (detailCat != null) {
                        ServiceCategoryDetailScreen(
                            category = detailCat,
                            language = language,
                            onBack = { activeCategoryDetail = null },
                            onRequestService = {
                                preselectedBookingCategory = detailCat
                                activeCategoryDetail = null
                                selectedTab = 1
                            },
                            onContactAdmin = {
                                activeCategoryDetail = null
                                selectedTab = 3
                            }
                        )
                    } else {
                        when (selectedTab) {
                            0 -> CustomerHomeScreen(
                                language = language,
                                onLanguageChange = onLanguageChange,
                                isGuest = isGuest,
                                onSignInClick = onRequestSignIn,
                                userProfile = userProfile,
                                categoriesState = categoriesState,
                                ordersState = ordersState,
                                notifications = notifications,
                                announcements = announcements,
                                businessSettings = businessSettings,
                                unreadChatCount = conversation?.unreadByCustomer ?: 0L,
                                onOpenNotifications = { showNotificationsDialog = true },
                                onSelectCategory = { cat -> activeCategoryDetail = cat },
                                onBookServiceClick = {
                                    preselectedBookingCategory = categoriesList.firstOrNull()
                                    selectedTab = 1
                                },
                                onMyOrdersClick = { selectedTab = 2 },
                                onContactAdminClick = { selectedTab = 3 }
                            )
                            1 -> ServiceRequestScreen(
                                language = language,
                                isGuest = isGuest,
                                onRequestSignIn = onRequestSignIn,
                                userProfile = userProfile,
                                categories = categoriesList,
                                preselectedCategory = preselectedBookingCategory,
                                bookingState = bookingState,
                                onSubmitOrder = { cName, phone, catId, catName, desc, addr, date, time, imgs, vid ->
                                    customerViewModel.submitServiceRequest(
                                        context, cName, phone, catId, catName, desc, addr, date, time, imgs, vid
                                    )
                                },
                                onContactAdminDirectly = { selectedTab = 3 },
                                onDismissSuccess = { customerViewModel.clearBookingState() },
                                onViewMyOrders = { selectedTab = 2 }
                            )
                            2 -> CustomerOrdersScreen(
                                language = language,
                                isGuest = isGuest,
                                onSignInClick = onRequestSignIn,
                                ordersState = ordersState,
                                onCancelPendingOrder = { customerViewModel.cancelOrder(it) },
                                onBookServiceClick = { selectedTab = 1 }
                            )
                            3 -> if (isGuest || chatViewModel == null) {
                                GuestChatPreviewScreen(
                                    phone = businessSettings.phone,
                                    onSignInClick = onRequestSignIn,
                                    onCallClick = {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${businessSettings.phone}"))
                                        context.startActivity(dialIntent)
                                    }
                                )
                            } else {
                                ChatScreen(
                                    viewModel = chatViewModel,
                                    headerTitle = strings.chatWithAdmin,
                                    headerSubtitle = businessSettings.phone,
                                    customerName = userProfile?.fullName ?: "Customer",
                                    customerPhone = userProfile?.phone ?: "",
                                    customerEmail = userProfile?.email ?: "",
                                    isAdminPerspective = false
                                )
                            }
                            4 -> CustomerProfileScreen(
                                language = language,
                                onLanguageChange = onLanguageChange,
                                isGuest = isGuest,
                                onSignInClick = onRequestSignIn,
                                userProfile = userProfile,
                                orders = ordersList,
                                onUpdateProfile = { name, phone, addr, photoUri ->
                                    customerViewModel.updateProfile(context, name, phone, addr, photoUri)
                                },
                                onNavigateOrders = { selectedTab = 2 },
                                onNavigateChat = { selectedTab = 3 },
                                onOpenNotifications = { showNotificationsDialog = true },
                                onLogout = onLogout
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNotificationsDialog) {
        NotificationsDialog(
            notifications = notifications,
            announcements = announcements,
            onMarkRead = { customerViewModel.markNotificationRead(it) },
            onDismiss = { showNotificationsDialog = false }
        )
    }
}

@Composable
private fun GuestChatPreviewScreen(
    phone: String,
    onSignInClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val strings = LocalStrings.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            com.example.ui.components.EmptyStateCard(
                icon = Icons.AutoMirrored.Filled.Chat,
                title = strings.chatWithAdmin,
                subtitle = strings.signInRequiredForChat
            )
            Spacer(modifier = Modifier.height(16.dp))
            com.example.ui.components.PrimaryYellowButton(
                text = strings.signInWithGoogle,
                icon = Icons.AutoMirrored.Filled.Login,
                onClick = onSignInClick,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onCallClick,
                border = BorderStroke(1.dp, ElectricYellow),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Filled.Call, contentDescription = null, tint = ElectricYellow)
                Spacer(modifier = Modifier.width(8.dp))
                Text(phone, color = ElectricYellow, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AdminMainScreen(
    currentUserId: String,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current

    val adminViewModel: AdminViewModel = viewModel(
        key = "admin_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                AdminViewModel(
                    userRepository = UserRepository(db),
                    categoryRepository = CategoryRepository(db),
                    orderRepository = OrderRepository(db),
                    chatRepository = ChatRepository(db),
                    notificationRepository = NotificationRepository(db),
                    settingsRepository = SettingsRepository(db)
                )
            }
        }
    )

    val stats by adminViewModel.stats.collectAsStateWithLifecycle()
    val customers by adminViewModel.customers.collectAsStateWithLifecycle()
    val ordersState by adminViewModel.orders.collectAsStateWithLifecycle()
    val conversations by adminViewModel.conversations.collectAsStateWithLifecycle()
    val categories by adminViewModel.categories.collectAsStateWithLifecycle()
    val adminNotifications by adminViewModel.adminNotifications.collectAsStateWithLifecycle()
    val announcements by adminViewModel.announcements.collectAsStateWithLifecycle()
    val businessSettings by adminViewModel.businessSettings.collectAsStateWithLifecycle()
    val actionBanner by adminViewModel.actionBanner.collectAsStateWithLifecycle()

    // Live system notification alerts for Admin
    val seenAdminNotifs = remember { mutableSetOf<String>() }
    LaunchedEffect(adminNotifications) {
        val newest = adminNotifications.firstOrNull { !it.isRead }
        if (newest != null && seenAdminNotifs.add(newest.id)) {
            LocalNotificationHelper.showNotification(
                context = context,
                id = newest.id.hashCode(),
                title = newest.title,
                body = newest.body
            )
        }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var activeConversation by remember { mutableStateOf<Conversation?>(null) }
    var overviewOrderDialog by remember { mutableStateOf<com.example.data.model.ServiceOrder?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionBanner) {
        if (actionBanner != null) {
            snackbarHostState.showSnackbar(actionBanner!!)
            adminViewModel.clearBanner()
        }
    }

    val navItems = listOf(
        NavTabItem(strings.navAdminOverview, Icons.Filled.Dashboard),
        NavTabItem(strings.navAdminOrders, Icons.Filled.ReceiptLong, badgeCount = stats.pendingRequests),
        NavTabItem(strings.navAdminChats, Icons.AutoMirrored.Filled.Chat, badgeCount = stats.unreadMessages.toInt()),
        NavTabItem(strings.navAdminCustomers, Icons.Filled.Group),
        NavTabItem(strings.navAdminManage, Icons.Filled.Tune)
    )

    val allOrders = (ordersState as? UiState.Success)?.data ?: emptyList()

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        val isWide = maxWidth >= 700.dp

        Scaffold(
            containerColor = DeepBlack,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                if (activeConversation == null) {
                    Surface(
                        color = CharcoalSurface,
                        border = BorderStroke(1.dp, SubtleBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                YusufLogoBadge(size = 38.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("YUSUF ELECTRONICS", style = MaterialTheme.typography.titleSmall, color = Color.White)
                                    Text("ADMIN PANEL", style = MaterialTheme.typography.labelSmall, color = ElectricYellow)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LanguageTogglePill(selected = language, onSelect = onLanguageChange)
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = onLogout, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = strings.logout, tint = DangerRed)
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (!isWide && activeConversation == null) {
                    NavigationBar(
                        containerColor = CharcoalSurface,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        navItems.forEachIndexed { idx, item ->
                            NavigationBarItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (item.badgeCount > 0) {
                                                Badge(containerColor = ElectricYellow, contentColor = DeepBlack) {
                                                    Text(item.badgeCount.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(item.icon, contentDescription = item.label)
                                    }
                                },
                                label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DeepBlack,
                                    selectedTextColor = ElectricYellow,
                                    indicatorColor = ElectricYellow,
                                    unselectedIconColor = LightGray,
                                    unselectedTextColor = LightGray
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWide && activeConversation == null) {
                    NavigationRail(
                        containerColor = CharcoalSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        navItems.forEachIndexed { idx, item ->
                            NavigationRailItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (item.badgeCount > 0) {
                                                Badge(containerColor = ElectricYellow, contentColor = DeepBlack) {
                                                    Text(item.badgeCount.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(item.icon, contentDescription = item.label)
                                    }
                                },
                                label = { Text(item.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = DeepBlack,
                                    selectedTextColor = ElectricYellow,
                                    indicatorColor = ElectricYellow,
                                    unselectedIconColor = LightGray,
                                    unselectedTextColor = LightGray
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    val conv = activeConversation
                    if (conv != null) {
                        BackHandler { activeConversation = null }
                        val adminChatVm: ChatViewModel = viewModel(
                            key = "admin_chat_${conv.customerId}",
                            factory = viewModelFactory {
                                initializer {
                                    val app = checkNotNull(this[APPLICATION_KEY]) as Application
                                    val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                                    ChatViewModel(
                                        chatRepository = ChatRepository(db),
                                        conversationId = conv.customerId,
                                        senderRole = "admin"
                                    )
                                }
                            }
                        )
                        ChatScreen(
                            viewModel = adminChatVm,
                            headerTitle = conv.customerName,
                            headerSubtitle = conv.customerPhone.ifBlank { conv.customerEmail },
                            customerName = conv.customerName,
                            customerPhone = conv.customerPhone,
                            customerEmail = conv.customerEmail,
                            isAdminPerspective = true,
                            onBack = { activeConversation = null }
                        )
                    } else {
                        when (selectedTab) {
                            0 -> AdminOverviewTab(
                                language = language,
                                stats = stats,
                                recentOrders = allOrders,
                                adminNotifications = adminNotifications,
                                onOpenOrder = { overviewOrderDialog = it },
                                onGoToOrders = { selectedTab = 1 },
                                onGoToChats = { selectedTab = 2 },
                                onMarkNotificationRead = { adminViewModel.markNotificationRead(it) }
                            )
                            1 -> AdminOrdersTab(
                                language = language,
                                ordersState = ordersState,
                                categories = categories,
                                onUpdateOrder = { ord, st, notes, sched, cost ->
                                    adminViewModel.updateOrder(ord, st, notes, sched, cost)
                                }
                            )
                            2 -> AdminChatsTab(
                                conversations = conversations,
                                onOpenConversation = { activeConversation = it }
                            )
                            3 -> AdminCustomersTab(
                                customers = customers,
                                allOrders = allOrders,
                                language = language,
                                onMessageCustomer = { cust ->
                                    val existing = conversations.firstOrNull { it.customerId == cust.uid }
                                    activeConversation = existing ?: Conversation(
                                        id = cust.uid,
                                        customerId = cust.uid,
                                        customerName = cust.fullName,
                                        customerPhone = cust.phone,
                                        customerEmail = cust.email
                                    )
                                }
                            )
                            4 -> AdminManageTab(
                                language = language,
                                categories = categories,
                                customers = customers,
                                announcements = announcements,
                                businessSettings = businessSettings,
                                onSaveCategory = { cat, isNew -> adminViewModel.saveCategory(cat, isNew) },
                                onToggleCategory = { id, active -> adminViewModel.toggleCategoryActive(id, active) },
                                onDeleteCategory = { adminViewModel.deleteCategory(it) },
                                onSendTargetedNotification = { uid, title, body ->
                                    adminViewModel.sendTargetedNotification(uid, title, body)
                                },
                                onSendAnnouncement = { title, body ->
                                    adminViewModel.sendAnnouncement(title, body)
                                },
                                onSaveBusinessSettings = { adminViewModel.saveBusinessSettings(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    overviewOrderDialog?.let { ord ->
        AdminOrderManageDialog(
            order = ord,
            language = language,
            onDismiss = { overviewOrderDialog = null },
            onSave = { st, notes, sched, cost ->
                adminViewModel.updateOrder(ord, st, notes, sched, cost)
                overviewOrderDialog = null
            }
        )
    }
}
