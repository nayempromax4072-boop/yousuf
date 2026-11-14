package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.unwrapFirestoreMessage
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.UiState
import com.example.data.repository.ChatRepository
import com.example.media.MediaHelper
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class ChatUploadState(
    val isUploading: Boolean = false,
    val progress: Float = 0f,
    val errorMessage: String? = null,
    val canRetry: Boolean = false
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val conversationId: String,
    private val senderRole: String
) : ViewModel() {

    val conversation: StateFlow<Conversation?> =
        chatRepository.observeConversation(conversationId)
            .catch { error ->
                if (error is CancellationException &&
                    generateSequence(error as Throwable) { it.cause }.none { it is FirebaseFirestoreException }
                ) throw error
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val messages: StateFlow<UiState<List<ChatMessage>>> =
        chatRepository.observeMessages(conversationId, limit = 120L)
            .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
            .catch { error ->
                val fsError = generateSequence(error as Throwable) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                emit(UiState.Error(unwrapFirestoreMessage(fsError ?: error, "Unable to load messages")))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _uploadState = MutableStateFlow(ChatUploadState())
    val uploadState: StateFlow<ChatUploadState> = _uploadState.asStateFlow()

    private var pendingRetryAction: (suspend () -> Unit)? = null

    fun markRead() {
        viewModelScope.launch {
            chatRepository.markConversationRead(conversationId, senderRole)
        }
    }

    fun sendText(
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        text: String
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val res = chatRepository.sendMessage(
                conversationId = conversationId,
                customerId = conversationId,
                customerName = customerName,
                customerPhone = customerPhone,
                customerEmail = customerEmail,
                senderRole = senderRole,
                messageType = "text",
                text = text
            )
            if (res.isFailure) {
                _uploadState.value = ChatUploadState(
                    errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Failed to send message."
                )
            }
        }
    }

    fun sendImage(
        context: Context,
        uri: Uri,
        caption: String,
        customerName: String,
        customerPhone: String,
        customerEmail: String
    ) {
        val action: suspend () -> Unit = {
            _uploadState.value = ChatUploadState(isUploading = true, progress = 0.05f)
            val bytes = MediaHelper.compressImageToJpeg(context, uri)
                ?: throw IllegalStateException("Could not compress selected image.")
            val url = chatRepository.uploadBytes(
                storageFolder = "chats/$conversationId",
                bytes = bytes,
                extension = "jpg",
                onProgress = { p ->
                    _uploadState.value = _uploadState.value.copy(progress = p.coerceIn(0.05f, 0.95f))
                }
            ).getOrThrow()
            chatRepository.sendMessage(
                conversationId = conversationId,
                customerId = conversationId,
                customerName = customerName,
                customerPhone = customerPhone,
                customerEmail = customerEmail,
                senderRole = senderRole,
                messageType = "image",
                text = caption,
                mediaUrl = url,
                mediaType = "image/jpeg"
            ).getOrThrow()
            _uploadState.value = ChatUploadState()
            pendingRetryAction = null
        }
        executeMediaUpload(action)
    }

    fun sendVideo(
        context: Context,
        uri: Uri,
        caption: String,
        customerName: String,
        customerPhone: String,
        customerEmail: String
    ) {
        val action: suspend () -> Unit = {
            val size = MediaHelper.getFileSizeBytes(context, uri)
            if (size > MediaHelper.MAX_VIDEO_BYTES) {
                throw IllegalArgumentException("Video must be 20 MB or smaller.")
            }
            _uploadState.value = ChatUploadState(isUploading = true, progress = 0.05f)
            val url = chatRepository.uploadContentUri(
                storageFolder = "chats/$conversationId",
                uri = uri,
                extension = "mp4",
                onProgress = { p ->
                    _uploadState.value = _uploadState.value.copy(progress = p.coerceIn(0.05f, 0.95f))
                }
            ).getOrThrow()
            chatRepository.sendMessage(
                conversationId = conversationId,
                customerId = conversationId,
                customerName = customerName,
                customerPhone = customerPhone,
                customerEmail = customerEmail,
                senderRole = senderRole,
                messageType = "video",
                text = caption,
                mediaUrl = url,
                mediaType = "video/mp4"
            ).getOrThrow()
            _uploadState.value = ChatUploadState()
            pendingRetryAction = null
        }
        executeMediaUpload(action)
    }

    fun sendVoice(
        file: File,
        durationSec: Long,
        customerName: String,
        customerPhone: String,
        customerEmail: String
    ) {
        val action: suspend () -> Unit = {
            _uploadState.value = ChatUploadState(isUploading = true, progress = 0.05f)
            val url = chatRepository.uploadFile(
                storageFolder = "chats/$conversationId",
                file = file,
                onProgress = { p ->
                    _uploadState.value = _uploadState.value.copy(progress = p.coerceIn(0.05f, 0.95f))
                }
            ).getOrThrow()
            chatRepository.sendMessage(
                conversationId = conversationId,
                customerId = conversationId,
                customerName = customerName,
                customerPhone = customerPhone,
                customerEmail = customerEmail,
                senderRole = senderRole,
                messageType = "voice",
                text = "",
                mediaUrl = url,
                mediaType = "audio/mp4",
                mediaDurationSec = durationSec
            ).getOrThrow()
            _uploadState.value = ChatUploadState()
            pendingRetryAction = null
        }
        executeMediaUpload(action)
    }

    fun retryLastUpload() {
        val action = pendingRetryAction ?: return
        executeMediaUpload(action)
    }

    fun dismissError() {
        _uploadState.value = _uploadState.value.copy(errorMessage = null)
    }

    private fun executeMediaUpload(action: suspend () -> Unit) {
        pendingRetryAction = action
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uploadState.value = ChatUploadState(
                    isUploading = false,
                    progress = 0f,
                    errorMessage = e.localizedMessage ?: "Upload failed. Check network or Firebase Storage.",
                    canRetry = true
                )
            }
        }
    }
}
