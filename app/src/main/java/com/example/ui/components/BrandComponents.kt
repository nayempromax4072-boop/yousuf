package com.example.ui.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WindPower
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.firebase.FirestoreMediaStore
import com.example.data.model.OrderStatus
import com.example.localization.AppLanguage
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElectricYellow
import com.example.ui.theme.LightGray
import com.example.ui.theme.StatusAccepted
import com.example.ui.theme.StatusCancelled
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusContacted
import com.example.ui.theme.StatusInProgress
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusScheduled
import com.example.ui.theme.SubtleBorder
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

fun categoryIconForKey(iconKey: String): ImageVector = when (iconKey.lowercase()) {
    "fan" -> Icons.Filled.WindPower
    "motor" -> Icons.Filled.Settings
    "ac" -> Icons.Filled.AcUnit
    "light" -> Icons.Filled.Lightbulb
    "wiring" -> Icons.Filled.ElectricalServices
    "cctv" -> Icons.Filled.Videocam
    "plumbing" -> Icons.Filled.Plumbing
    "building_electric" -> Icons.Filled.Apartment
    "maintenance" -> Icons.Filled.Handyman
    "bolt" -> Icons.Filled.ElectricBolt
    else -> Icons.Filled.Build
}

fun orderStatusColor(status: OrderStatus): Color = when (status) {
    OrderStatus.PENDING -> StatusPending
    OrderStatus.ACCEPTED -> StatusAccepted
    OrderStatus.CONTACTED -> StatusContacted
    OrderStatus.WORK_SCHEDULED -> StatusScheduled
    OrderStatus.IN_PROGRESS -> StatusInProgress
    OrderStatus.COMPLETED -> StatusCompleted
    OrderStatus.CANCELLED -> StatusCancelled
}

fun formatTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return "Just now"
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}

@Composable
fun YusufLogoBadge(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.yusuf_logo),
        contentDescription = "Yusuf Electronics Logo",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, ElectricYellow.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
    )
}

@Composable
fun StatusBadge(
    status: OrderStatus,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val color = orderStatusColor(status)
    Surface(
        color = color.copy(alpha = 0.16f),
        contentColor = color,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Text(
            text = status.localized(language),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PrimaryYellowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    testTag: String = ""
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = ElectricYellow,
            contentColor = DeepBlack,
            disabledContainerColor = ElectricYellow.copy(alpha = 0.4f),
            disabledContentColor = DeepBlack.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier
            .height(52.dp)
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = DeepBlack,
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MetricTile(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color = ElectricYellow,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        border = BorderStroke(1.dp, SubtleBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = LightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        border = BorderStroke(1.dp, SubtleBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ElectricYellow.copy(alpha = 0.14f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ElectricYellow,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = LightGray
            )
        }
    }
}

@Composable
fun MediaSourceChooserDialog(
    title: String,
    cameraLabel: String,
    galleryLabel: String,
    cancelLabel: String,
    isVideo: Boolean,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CharcoalSurface,
        title = { Text(title, color = ElectricYellow, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        onDismiss()
                        onCameraClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricYellow,
                        contentColor = DeepBlack
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = if (isVideo) Icons.Filled.Videocam else Icons.Filled.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(cameraLabel, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onGalleryClick()
                    },
                    border = BorderStroke(1.dp, ElectricYellow),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        tint = ElectricYellow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(galleryLabel, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel, color = LightGray)
            }
        }
    )
}

@Composable
fun FullScreenImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepBlack.copy(alpha = 0.96f))
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Attachment Image Preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CharcoalSurface)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close Preview",
                    tint = ElectricYellow
                )
            }
        }
    }
}

@Composable
fun FullScreenVideoDialog(
    videoUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var resolvedVideoPath by remember(videoUrl) {
        mutableStateOf<String?>(if (FirestoreMediaStore.isFsMediaUrl(videoUrl)) null else videoUrl)
    }
    var videoError by remember(videoUrl) { mutableStateOf<String?>(null) }

    LaunchedEffect(videoUrl) {
        if (FirestoreMediaStore.isFsMediaUrl(videoUrl)) {
            val db = FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            )
            val file = FirestoreMediaStore.resolveToCachedFile(context, db, videoUrl)
            if (file != null) {
                resolvedVideoPath = file.absolutePath
            } else {
                videoError = "Unable to load video."
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepBlack)
        ) {
            val activePath = resolvedVideoPath
            when {
                videoError != null -> {
                    Text(
                        text = videoError ?: "Video error",
                        color = DangerRed,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                activePath == null -> {
                    CircularProgressIndicator(
                        color = ElectricYellow,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val uri = if (activePath.startsWith("/")) {
                                    Uri.fromFile(File(activePath))
                                } else {
                                    Uri.parse(activePath)
                                }
                                setVideoURI(uri)
                                val controller = MediaController(ctx)
                                controller.setAnchorView(this)
                                setMediaController(controller)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = false
                                    start()
                                }
                                setOnErrorListener { _, _, _ ->
                                    videoError = "Unable to play video."
                                    true
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CharcoalSurface)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close Video",
                    tint = ElectricYellow
                )
            }
        }
    }
}
