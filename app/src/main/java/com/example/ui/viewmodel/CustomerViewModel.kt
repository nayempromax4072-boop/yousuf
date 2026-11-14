package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.unwrapFirestoreMessage
import com.example.data.model.Announcement
import com.example.data.model.BusinessSettings
import com.example.data.model.Conversation
import com.example.data.model.DefaultServiceCategories
import com.example.data.model.NotificationItem
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
import com.example.media.MediaHelper
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BookingFormState(
    val isSubmitting: Boolean = false,
    val uploadProgress: Float = 0f,
    val submittedOrderCode: String? = null,
    val errorMessage: String? = null
)

class CustomerViewModel(
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val orderRepository: OrderRepository,
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository,
    private val currentUserId: String
) : ViewModel() {

    private val isGuest = currentUserId.isBlank()

    val userProfile: StateFlow<UserProfile?> = if (isGuest) {
        MutableStateFlow<UserProfile?>(null).asStateFlow()
    } else {
        userRepository.observeUser(currentUserId)
            .catch { error ->
                if (error is CancellationException &&
                    generateSequence(error as Throwable) { it.cause }.none { it is FirebaseFirestoreException }
                ) throw error
                Log.w(TAG, "Error observing user", error)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)
    }

    val categories: StateFlow<UiState<List<ServiceCategory>>> = if (isGuest) {
        MutableStateFlow<UiState<List<ServiceCategory>>>(UiState.Success(DefaultServiceCategories.all)).asStateFlow()
    } else {
        categoryRepository.observeCategories(includeInactive = false)
            .map<List<ServiceCategory>, UiState<List<ServiceCategory>>> { UiState.Success(it) }
            .catch { error ->
                val fsError = generateSequence(error as Throwable) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                emit(UiState.Error(unwrapFirestoreMessage(fsError ?: error, "Failed to load categories")))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    }

    val orders: StateFlow<UiState<List<ServiceOrder>>> = if (isGuest) {
        MutableStateFlow<UiState<List<ServiceOrder>>>(UiState.Success(emptyList())).asStateFlow()
    } else {
        orderRepository.observeCustomerOrders(currentUserId)
            .map<List<ServiceOrder>, UiState<List<ServiceOrder>>> { UiState.Success(it) }
            .catch { error ->
                val fsError = generateSequence(error as Throwable) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                emit(UiState.Error(unwrapFirestoreMessage(fsError ?: error, "Failed to load orders")))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    }

    val notifications: StateFlow<List<NotificationItem>> = if (isGuest) {
        flowOf(emptyList<NotificationItem>())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
    } else {
        notificationRepository.observeNotifications(currentUserId)
            .catch { error ->
                if (error is CancellationException &&
                    generateSequence(error as Throwable) { it.cause }.none { it is FirebaseFirestoreException }
                ) throw error
                Log.w(TAG, "Error observing notifications", error)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
    }

    val announcements: StateFlow<List<Announcement>> = if (isGuest) {
        flowOf(emptyList<Announcement>())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
    } else {
        notificationRepository.observeAnnouncements()
            .catch { error ->
                if (error is CancellationException &&
                    generateSequence(error as Throwable) { it.cause }.none { it is FirebaseFirestoreException }
                ) throw error
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
    }

    val businessSettings: StateFlow<BusinessSettings> = if (isGuest) {
        MutableStateFlow(BusinessSettings()).asStateFlow()
    } else {
        settingsRepository.observeBusinessSettings()
            .catch { error ->
                if (error is CancellationException &&
                    generateSequence(error as Throwable) { it.cause }.none { it is FirebaseFirestoreException }
                ) throw error
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), BusinessSettings())
    }

    val conversation: StateFlow<Conversation?> = if (isGuest) {
        MutableStateFlow<Conversation?>(null).asStateFlow()
    } else {
        chatRepository.observeConversation(currentUserId)
            .catch { error ->
                if (error is CancellationException &&
                    generateSequence(error as Throwable) { it.cause }.none { it is FirebaseFirestoreException }
                ) throw error
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)
    }

    private val _bookingState = MutableStateFlow(BookingFormState())
    val bookingState: StateFlow<BookingFormState> = _bookingState.asStateFlow()

    private val _profileMessage = MutableStateFlow<String?>(null)
    val profileMessage: StateFlow<String?> = _profileMessage.asStateFlow()

    fun submitServiceRequest(
        context: Context,
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
    ) {
        viewModelScope.launch {
            _bookingState.value = BookingFormState(isSubmitting = true, uploadProgress = 0.05f)
            try {
                val uploadedImages = mutableListOf<String>()
                for ((index, uri) in imageUris.take(3).withIndex()) {
                    val bytes = MediaHelper.compressImageToJpeg(context, uri)
                        ?: throw IllegalStateException("Unable to read selected image.")
                    val uploadRes = chatRepository.uploadBytes(
                        storageFolder = "orders/$currentUserId",
                        bytes = bytes,
                        extension = "jpg",
                        onProgress = { p ->
                            val base = (index.toFloat() / (imageUris.size + 1).coerceAtLeast(1))
                            _bookingState.value = _bookingState.value.copy(
                                uploadProgress = (base + p * 0.3f).coerceIn(0.05f, 0.9f)
                            )
                        }
                    )
                    uploadedImages.add(uploadRes.getOrThrow())
                }

                var uploadedVideoUrl = ""
                if (videoUri != null) {
                    val size = MediaHelper.getFileSizeBytes(context, videoUri)
                    if (size > MediaHelper.MAX_VIDEO_BYTES) {
                        throw IllegalArgumentException("Video exceeds 20 MB size limit.")
                    }
                    uploadedVideoUrl = chatRepository.uploadContentUri(
                        storageFolder = "orders/$currentUserId",
                        uri = videoUri,
                        extension = "mp4",
                        onProgress = { p ->
                            _bookingState.value = _bookingState.value.copy(
                                uploadProgress = (0.4f + p * 0.5f).coerceIn(0.4f, 0.95f)
                            )
                        }
                    ).getOrThrow()
                }

                val orderResult = orderRepository.submitOrder(
                    customerName = customerName,
                    phone = phone,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    description = description,
                    address = address,
                    preferredDate = preferredDate,
                    preferredTime = preferredTime,
                    attachmentUrls = uploadedImages,
                    videoUrl = uploadedVideoUrl
                )
                orderResult.onSuccess { order ->
                    _bookingState.value = BookingFormState(
                        isSubmitting = false,
                        uploadProgress = 1f,
                        submittedOrderCode = order.orderCode
                    )
                }.onFailure { err ->
                    _bookingState.value = BookingFormState(
                        isSubmitting = false,
                        errorMessage = err.localizedMessage ?: "Failed to submit request."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bookingState.value = BookingFormState(
                    isSubmitting = false,
                    errorMessage = e.localizedMessage ?: "Attachment upload or booking failed."
                )
            }
        }
    }

    fun clearBookingState() {
        _bookingState.value = BookingFormState()
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            orderRepository.cancelPendingOrder(orderId)
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    fun updateProfile(
        context: Context,
        fullName: String,
        phone: String,
        address: String,
        newPhotoUri: Uri? = null
    ) {
        viewModelScope.launch {
            var photoUrl: String? = null
            if (newPhotoUri != null) {
                val bytes = MediaHelper.compressImageToJpeg(context, newPhotoUri, maxDim = 512, quality = 80)
                if (bytes != null) {
                    photoUrl = chatRepository.uploadBytes(
                        storageFolder = "profiles/$currentUserId",
                        bytes = bytes,
                        extension = "jpg"
                    ).getOrNull()
                }
            }
            val result = userRepository.updateProfile(
                uid = currentUserId,
                fullName = fullName,
                phone = phone,
                address = address,
                photoUrl = photoUrl
            )
            _profileMessage.value = if (result.isSuccess) {
                "Profile updated successfully"
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Failed to update profile"
            }
        }
    }

    fun clearProfileMessage() {
        _profileMessage.value = null
    }

    private companion object {
        const val TAG = "CustomerVM"
    }
}
