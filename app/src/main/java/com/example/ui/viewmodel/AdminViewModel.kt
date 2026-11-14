package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.unwrapFirestoreMessage
import com.example.data.model.Announcement
import com.example.data.model.BusinessSettings
import com.example.data.model.Conversation
import com.example.data.model.NotificationItem
import com.example.data.model.OrderStatus
import com.example.data.model.ServiceCategory
import com.example.data.model.ServiceOrder
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.CategoryRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdminDashboardStats(
    val totalCustomers: Int = 0,
    val totalRequests: Int = 0,
    val pendingRequests: Int = 0,
    val acceptedRequests: Int = 0,
    val activeJobs: Int = 0,
    val completedJobs: Int = 0,
    val unreadMessages: Long = 0L
)

class AdminViewModel(
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val orderRepository: OrderRepository,
    private val chatRepository: ChatRepository,
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            categoryRepository.seedDefaultCategoriesIfEmpty()
        }
    }

    val customers: StateFlow<List<UserProfile>> =
        userRepository.observeAllCustomers()
            .catch { error ->
                if (error is CancellationException &&
                    generateSequence(error as Throwable) { it.cause }.none { it is FirebaseFirestoreException }
                ) throw error
                Log.w(TAG, "Failed observing customers", error)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val orders: StateFlow<UiState<List<ServiceOrder>>> =
        orderRepository.observeAllOrders()
            .map<List<ServiceOrder>, UiState<List<ServiceOrder>>> { UiState.Success(it) }
            .catch { error ->
                val fsError = generateSequence(error as Throwable) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                emit(UiState.Error(unwrapFirestoreMessage(fsError ?: error, "Failed to load orders")))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val conversations: StateFlow<List<Conversation>> =
        chatRepository.observeAllConversations()
            .catch { error ->
                if (error is CancellationException &&
                    generateSequence(error as Throwable) { it.cause }.none { it is FirebaseFirestoreException }
                ) throw error
                Log.w(TAG, "Failed observing conversations", error)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val categories: StateFlow<List<ServiceCategory>> =
        categoryRepository.observeCategories(includeInactive = true)
            .catch { error ->
                if (error is CancellationException &&
                    generateSequence(error as Throwable) { it.cause }.none { it is FirebaseFirestoreException }
                ) throw error
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminNotifications: StateFlow<List<NotificationItem>> =
        notificationRepository.observeNotifications("ADMIN")
            .catch { error ->
                if (error is CancellationException &&
                    generateSequence(error as Throwable) { it.cause }.none { it is FirebaseFirestoreException }
                ) throw error
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val announcements: StateFlow<List<Announcement>> =
        notificationRepository.observeAnnouncements()
            .catch { error ->
                if (error is CancellationException &&
                    generateSequence(error as Throwable) { it.cause }.none { it is FirebaseFirestoreException }
                ) throw error
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val businessSettings: StateFlow<BusinessSettings> =
        settingsRepository.observeBusinessSettings()
            .catch { error ->
                if (error is CancellationException &&
                    generateSequence(error as Throwable) { it.cause }.none { it is FirebaseFirestoreException }
                ) throw error
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), BusinessSettings())

    val stats: StateFlow<AdminDashboardStats> = combine(
        customers,
        orders,
        conversations
    ) { customerList, ordersState, convList ->
        val allOrders = (ordersState as? UiState.Success)?.data ?: emptyList()
        val onlyCustomers = customerList.filter { it.role != "admin" }
        AdminDashboardStats(
            totalCustomers = onlyCustomers.size,
            totalRequests = allOrders.size,
            pendingRequests = allOrders.count { it.status == OrderStatus.PENDING.value },
            acceptedRequests = allOrders.count { it.status == OrderStatus.ACCEPTED.value },
            activeJobs = allOrders.count {
                it.status in setOf(
                    OrderStatus.ACCEPTED.value,
                    OrderStatus.CONTACTED.value,
                    OrderStatus.WORK_SCHEDULED.value,
                    OrderStatus.IN_PROGRESS.value
                )
            },
            completedJobs = allOrders.count { it.status == OrderStatus.COMPLETED.value },
            unreadMessages = convList.sumOf { it.unreadByAdmin }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AdminDashboardStats())

    private val _actionBanner = MutableStateFlow<String?>(null)
    val actionBanner: StateFlow<String?> = _actionBanner.asStateFlow()

    fun clearBanner() {
        _actionBanner.value = null
    }

    fun updateOrder(
        order: ServiceOrder,
        newStatus: OrderStatus,
        adminNotes: String,
        scheduledDate: String,
        estimatedCostBdt: Long
    ) {
        viewModelScope.launch {
            val res = orderRepository.adminUpdateOrder(
                orderId = order.id,
                customerId = order.customerId,
                orderCode = order.orderCode,
                newStatus = newStatus,
                adminNotes = adminNotes,
                scheduledDate = scheduledDate,
                estimatedCostBdt = estimatedCostBdt
            )
            _actionBanner.value = if (res.isSuccess) {
                "Order ${order.orderCode} updated to ${newStatus.value}"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to update order"
            }
        }
    }

    fun saveCategory(category: ServiceCategory, isNew: Boolean) {
        viewModelScope.launch {
            val res = categoryRepository.saveCategory(category, isNew)
            _actionBanner.value = if (res.isSuccess) {
                "Category saved"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to save category"
            }
        }
    }

    fun toggleCategoryActive(categoryId: String, isActive: Boolean) {
        viewModelScope.launch {
            categoryRepository.toggleCategoryActive(categoryId, isActive)
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(categoryId)
        }
    }

    fun sendTargetedNotification(customerUid: String, title: String, body: String) {
        viewModelScope.launch {
            val res = notificationRepository.sendTargetedNotification(
                targetUserId = customerUid,
                title = title,
                body = body
            )
            _actionBanner.value = if (res.isSuccess) "Notification sent" else "Failed to send notification"
        }
    }

    fun sendAnnouncement(title: String, body: String) {
        viewModelScope.launch {
            val res = notificationRepository.sendAnnouncement(title, body)
            _actionBanner.value = if (res.isSuccess) "Announcement broadcast to all customers" else "Failed to broadcast"
        }
    }

    fun saveBusinessSettings(settings: BusinessSettings) {
        viewModelScope.launch {
            val res = settingsRepository.saveBusinessSettings(settings)
            _actionBanner.value = if (res.isSuccess) "Business settings saved" else "Failed to save settings"
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(id)
        }
    }

    private companion object {
        const val TAG = "AdminVM"
    }
}
