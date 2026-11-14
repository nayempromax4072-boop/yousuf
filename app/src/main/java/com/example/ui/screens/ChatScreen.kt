package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.ChatMessage
import com.example.data.model.UiState
import com.example.localization.LocalStrings
import com.example.media.VoicePlayerManager
import com.example.media.VoiceRecorderManager
import com.example.media.observeNetworkOnline
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FullScreenImageDialog
import com.example.ui.components.FullScreenVideoDialog
import com.example.ui.components.YusufLogoBadge
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElectricYellow
import com.example.ui.theme.ElevatedCharcoal
import com.example.ui.theme.LightGray
import com.example.ui.theme.MutedGray
import com.example.ui.theme.SubtleBorder
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import java.io.File

private sealed interface PendingAttachment {
    data class Image(val uri: Uri) : PendingAttachment
    data class Video(val uri: Uri) : PendingAttachment
    data class Voice(val file: File, val durationSec: Long) : PendingAttachment
}

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    headerTitle: String,
    headerSubtitle: String,
    customerName: String,
    customerPhone: String,
    customerEmail: String,
    isAdminPerspective: Boolean,
    onBack: (() -> Unit)? = null
) {
    val strings = LocalStrings.current
    val context = LocalContext.current

    val messagesState by viewModel.messages.collectAsStateWithLifecycle()
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
    val isOnline by observeNetworkOnline(context).collectAsStateWithLifecycle(initialValue = true)

    var textInput by remember { mutableStateOf("") }
    var pendingAttachment by remember { mutableStateOf<PendingAttachment?>(null) }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    var fullScreenVideoUrl by remember { mutableStateOf<String?>(null) }

    // Voice recording & playback
    val recorderManager = remember(context) { VoiceRecorderManager(context) }
    val playerManager = remember { VoicePlayerManager() }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableLongStateOf(0L) }
    var activePlayingKey by remember { mutableStateOf<String?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var audioError by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    DisposableEffect(Unit) {
        onDispose {
            recorderManager.stopAndRelease()
            playerManager.stop()
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0L
            while (isRecording) {
                delay(1000L)
                recordingSeconds += 1L
            }
        }
    }

    // Auto-mark read when messages arrive
    LaunchedEffect(messagesState) {
        if (messagesState is UiState.Success) {
            val list = (messagesState as UiState.Success<List<ChatMessage>>).data
            if (list.isNotEmpty()) {
                viewModel.markRead()
                listState.animateScrollToItem(list.lastIndex)
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pendingAttachment = PendingAttachment.Image(uri)
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pendingAttachment = PendingAttachment.Video(uri)
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val started = recorderManager.startRecording() != null
            if (started) isRecording = true else audioError = "Unable to start microphone recorder."
        } else {
            audioError = "Microphone permission is required to record voice messages."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        // Chat Header
        Surface(
            color = CharcoalSurface,
            border = BorderStroke(1.dp, SubtleBorder)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ElectricYellow
                        )
                    }
                } else {
                    YusufLogoBadge(size = 40.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = headerSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricYellow,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Offline Banner
        if (!isOnline) {
            Surface(color = DangerRed.copy(alpha = 0.2f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.WifiOff,
                        contentDescription = null,
                        tint = Color(0xFFFCA5A5),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.offlineNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFCA5A5)
                    )
                }
            }
        }

        // Upload Progress & Retry Banner
        if (uploadState.isUploading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CharcoalSurface)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Uploading media ${(uploadState.progress * 100).toInt()}%...",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricYellow
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { uploadState.progress },
                    color = ElectricYellow,
                    trackColor = SubtleBorder,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        val errMsg = uploadState.errorMessage ?: audioError
        if (!errMsg.isNullOrBlank()) {
            Surface(
                color = DangerRed.copy(alpha = 0.16f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = errMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFCA5A5),
                        modifier = Modifier.weight(1f)
                    )
                    if (uploadState.canRetry) {
                        TextButton(onClick = { viewModel.retryLastUpload() }) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = ElectricYellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.retryUpload, color = ElectricYellow)
                        }
                    } else {
                        IconButton(onClick = {
                            viewModel.dismissError()
                            audioError = null
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = LightGray)
                        }
                    }
                }
            }
        }

        // Messages List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (val state = messagesState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(
                        color = ElectricYellow,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        EmptyStateCard(
                            icon = Icons.Filled.WifiOff,
                            title = "Unable to load chat",
                            subtitle = state.message
                        )
                    }
                }
                is UiState.Success -> {
                    val items = state.data
                    if (items.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            EmptyStateCard(
                                icon = Icons.Filled.GraphicEq,
                                title = strings.noMessagesYet,
                                subtitle = strings.tagline
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(items, key = { it.id }) { msg ->
                                val isMine = if (isAdminPerspective) {
                                    msg.senderRole == "admin"
                                } else {
                                    msg.senderRole == "customer"
                                }
                                ChatBubbleItem(
                                    message = msg,
                                    isMine = isMine,
                                    isPlayingVoice = activePlayingKey == msg.id && isAudioPlaying,
                                    onOpenImage = { fullScreenImageUrl = msg.mediaUrl },
                                    onOpenVideo = { fullScreenVideoUrl = msg.mediaUrl },
                                    onToggleVoice = {
                                        playerManager.togglePlay(
                                            key = msg.id,
                                            source = msg.mediaUrl,
                                            onStateChanged = { k, playing ->
                                                activePlayingKey = k
                                                isAudioPlaying = playing
                                            },
                                            onError = { audioError = it }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pending Attachment Preview Before Sending
        pendingAttachment?.let { attachment ->
            Surface(
                color = ElevatedCharcoal,
                border = BorderStroke(1.dp, ElectricYellow.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    when (attachment) {
                        is PendingAttachment.Image -> {
                            AsyncImage(
                                model = attachment.uri,
                                contentDescription = "Selected photo preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = strings.previewBeforeSend + " (Photo)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        is PendingAttachment.Video -> {
                            Icon(
                                imageVector = Icons.Filled.VideoFile,
                                contentDescription = null,
                                tint = ElectricYellow,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = strings.previewBeforeSend + " (Video)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        is PendingAttachment.Voice -> {
                            val previewKey = "preview_${attachment.file.name}"
                            IconButton(
                                onClick = {
                                    playerManager.togglePlay(
                                        key = previewKey,
                                        source = attachment.file.absolutePath,
                                        onStateChanged = { k, playing ->
                                            activePlayingKey = k
                                            isAudioPlaying = playing
                                        },
                                        onError = { audioError = it }
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = if (activePlayingKey == previewKey && isAudioPlaying) {
                                        Icons.Filled.Pause
                                    } else {
                                        Icons.Filled.PlayArrow
                                    },
                                    contentDescription = "Preview voice message",
                                    tint = ElectricYellow
                                )
                            }
                            Text(
                                text = "Voice clip (${attachment.durationSec}s)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    IconButton(onClick = { pendingAttachment = null }) {
                        Icon(Icons.Filled.Close, contentDescription = strings.cancel, tint = LightGray)
                    }
                    Button(
                        onClick = {
                            val caption = textInput
                            textInput = ""
                            when (val current = pendingAttachment) {
                                is PendingAttachment.Image -> viewModel.sendImage(
                                    context, current.uri, caption, customerName, customerPhone, customerEmail
                                )
                                is PendingAttachment.Video -> viewModel.sendVideo(
                                    context, current.uri, caption, customerName, customerPhone, customerEmail
                                )
                                is PendingAttachment.Voice -> viewModel.sendVoice(
                                    current.file, current.durationSec, customerName, customerPhone, customerEmail
                                )
                                null -> Unit
                            }
                            pendingAttachment = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricYellow,
                            contentColor = DeepBlack
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(strings.send, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Composer Bar
        Surface(
            color = CharcoalSurface,
            border = BorderStroke(1.dp, SubtleBorder)
        ) {
            if (isRecording) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(DangerRed)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${strings.recordingVoice} ${recordingSeconds}s",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                    }
                    Button(
                        onClick = {
                            isRecording = false
                            val (file, dur) = recorderManager.stopRecording()
                            if (file != null) {
                                pendingAttachment = PendingAttachment.Voice(file, dur)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricYellow,
                            contentColor = DeepBlack
                        )
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.stopRecording, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    IconButton(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_attach_image")
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = "Send Image", tint = ElectricYellow)
                    }
                    IconButton(
                        onClick = {
                            videoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_attach_video")
                    ) {
                        Icon(Icons.Filled.VideoFile, contentDescription = "Send Video", tint = ElectricYellow)
                    }
                    IconButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                val started = recorderManager.startRecording() != null
                                if (started) isRecording = true else audioError = "Microphone unavailable."
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_record_voice")
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = strings.recordVoice, tint = ElectricYellow)
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text(strings.typeMessageHint, color = MutedGray) },
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricYellow,
                            unfocusedBorderColor = SubtleBorder,
                            focusedContainerColor = DeepBlack,
                            unfocusedContainerColor = DeepBlack,
                            cursorColor = ElectricYellow
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            val trimmed = textInput.trim()
                            if (trimmed.isNotEmpty()) {
                                textInput = ""
                                viewModel.sendText(customerName, customerPhone, customerEmail, trimmed)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ElectricYellow)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = strings.send,
                            tint = DeepBlack
                        )
                    }
                }
            }
        }
    }

    fullScreenImageUrl?.let { url ->
        FullScreenImageDialog(imageUrl = url, onDismiss = { fullScreenImageUrl = null })
    }
    fullScreenVideoUrl?.let { url ->
        FullScreenVideoDialog(videoUrl = url, onDismiss = { fullScreenVideoUrl = null })
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessage,
    isMine: Boolean,
    isPlayingVoice: Boolean,
    onOpenImage: () -> Unit,
    onOpenVideo: () -> Unit,
    onToggleVoice: () -> Unit
) {
    val bubbleColor = if (isMine) ElectricYellow else CharcoalSurface
    val textColor = if (isMine) DeepBlack else Color.White
    val subColor = if (isMine) DeepBlack.copy(alpha = 0.7f) else LightGray

    Row(
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            border = if (isMine) null else BorderStroke(1.dp, SubtleBorder),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMine) 18.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 18.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                when (message.messageType) {
                    "image" -> {
                        if (message.mediaUrl.isNotBlank()) {
                            AsyncImage(
                                model = message.mediaUrl,
                                contentDescription = "Chat image attachment",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOpenImage() }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                    "video" -> {
                        Surface(
                            color = DeepBlack.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clickable { onOpenVideo() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(ElectricYellow)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Play video",
                                        tint = DeepBlack
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Tap to watch video",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    "voice" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isMine) DeepBlack.copy(alpha = 0.12f) else ElevatedCharcoal)
                                .clickable { onToggleVoice() }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isMine) DeepBlack else ElectricYellow)
                            ) {
                                Icon(
                                    imageVector = if (isPlayingVoice) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play voice message",
                                    tint = if (isMine) ElectricYellow else DeepBlack
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isPlayingVoice) "Playing voice..." else "Voice Message",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = textColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${message.mediaDurationSec}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = subColor
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = subColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatTimestamp(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = subColor
                    )
                    if (isMine) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (message.isRead) Icons.Filled.DoneAll else Icons.Filled.Check,
                            contentDescription = if (message.isRead) "Read" else "Sent",
                            tint = if (message.isRead) Color(0xFF0F766E) else subColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
