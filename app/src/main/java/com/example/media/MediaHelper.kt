package com.example.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.firebase.FirestoreMediaStore
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

object MediaHelper {
    const val MAX_VIDEO_BYTES = FirestoreMediaStore.MAX_MEDIA_BYTES // 10 MB

    fun createCameraTempUri(context: Context, prefix: String, extension: String): Uri {
        val dir = File(context.cacheDir, "camera_media").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.$extension")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    suspend fun compressImageToJpeg(context: Context, uri: Uri, maxDim: Int = 1080, quality: Int = 78): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val rotationDegrees = try {
                    context.contentResolver.openInputStream(uri)?.use { exifStream ->
                        when (ExifInterface(exifStream).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                            else -> 0f
                        }
                    } ?: 0f
                } catch (_: Exception) {
                    0f
                }

                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val original = inputStream.use { BitmapFactory.decodeStream(it) } ?: return@withContext null

                val orientedBitmap = if (rotationDegrees != 0f) {
                    val matrix = Matrix().apply { postRotate(rotationDegrees) }
                    Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true).also {
                        if (it !== original) original.recycle()
                    }
                } else {
                    original
                }

                val width = orientedBitmap.width
                val height = orientedBitmap.height
                val scale = if (width > maxDim || height > maxDim) {
                    minOf(maxDim.toFloat() / width, maxDim.toFloat() / height)
                } else {
                    1f
                }

                val targetBitmap = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        orientedBitmap,
                        (width * scale).toInt().coerceAtLeast(1),
                        (height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                } else {
                    orientedBitmap
                }

                val out = ByteArrayOutputStream()
                targetBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                if (targetBitmap !== orientedBitmap) targetBitmap.recycle()
                orientedBitmap.recycle()
                out.toByteArray()
            } catch (_: Exception) {
                null
            }
        }

    fun getFileSizeBytes(context: Context, uri: Uri): Long {
        val queried = try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0 && !cursor.isNull(idx)) cursor.getLong(idx) else -1L
                } else -1L
            } ?: -1L
        } catch (_: Exception) {
            -1L
        }
        if (queried > 0L) return queried

        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        } catch (_: Exception) {
            -1L
        }
    }
}

class CaptureCompactVideo : ActivityResultContract<Uri, Boolean>() {
    override fun createIntent(context: Context, input: Uri): Intent {
        return Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, input)
            putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0)
            putExtra(MediaStore.EXTRA_DURATION_LIMIT, 45)
            putExtra(MediaStore.EXTRA_SIZE_LIMIT, FirestoreMediaStore.MAX_MEDIA_BYTES)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }
}

class VoiceRecorderManager(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMs: Long = 0L

    fun startRecording(): File? {
        stopAndRelease()
        return try {
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(32000)
                setAudioSamplingRate(16000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            outputFile = file
            startTimeMs = System.currentTimeMillis()
            file
        } catch (_: Exception) {
            stopAndRelease()
            null
        }
    }

    fun stopRecording(): Pair<File?, Long> {
        val durationSec = ((System.currentTimeMillis() - startTimeMs) / 1000L).coerceAtLeast(1L)
        val file = outputFile
        try {
            recorder?.stop()
        } catch (_: Exception) {
            file?.delete()
            stopAndRelease()
            return null to 0L
        }
        stopAndRelease()
        return if (file != null && file.exists() && file.length() > 0) {
            file to durationSec
        } else {
            null to 0L
        }
    }

    fun stopAndRelease() {
        try {
            recorder?.reset()
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
    }
}

class VoicePlayerManager {
    private var player: MediaPlayer? = null
    var activeKey: String? = null
        private set

    fun togglePlay(
        context: Context,
        scope: CoroutineScope,
        key: String,
        source: String,
        onStateChanged: (playingKey: String?, isPlaying: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        if (activeKey == key && player != null) {
            val current = player!!
            if (current.isPlaying) {
                current.pause()
                onStateChanged(key, false)
            } else {
                current.start()
                onStateChanged(key, true)
            }
            return
        }

        stop()
        if (source.isBlank()) {
            onError("Audio source unavailable.")
            return
        }

        if (FirestoreMediaStore.isFsMediaUrl(source)) {
            scope.launch {
                try {
                    val db = FirebaseFirestore.getInstance(
                        context.applicationContext.getString(R.string.firestore_database_id)
                    )
                    val file = FirestoreMediaStore.resolveToCachedFile(context, db, source)
                    if (file == null) {
                        onError("Unable to load voice message.")
                        return@launch
                    }
                    startPlayback(key, file.absolutePath, onStateChanged, onError)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    onError(e.localizedMessage ?: "Audio playback error.")
                }
            }
        } else {
            startPlayback(key, source, onStateChanged, onError)
        }
    }

    private fun startPlayback(
        key: String,
        resolvedSource: String,
        onStateChanged: (playingKey: String?, isPlaying: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val mp = MediaPlayer().apply {
                setDataSource(resolvedSource)
                setOnPreparedListener {
                    it.start()
                    activeKey = key
                    onStateChanged(key, true)
                }
                setOnCompletionListener {
                    activeKey = null
                    onStateChanged(null, false)
                    release()
                    player = null
                }
                setOnErrorListener { _, _, _ ->
                    activeKey = null
                    onStateChanged(null, false)
                    onError("Unable to play audio clip.")
                    true
                }
                prepareAsync()
            }
            player = mp
        } catch (e: Exception) {
            activeKey = null
            onStateChanged(null, false)
            onError(e.localizedMessage ?: "Audio playback error.")
        }
    }

    fun stop() {
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        activeKey = null
    }
}

fun observeNetworkOnline(context: Context): Flow<Boolean> = callbackFlow {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    fun checkCurrent(): Boolean {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    trySend(checkCurrent())

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            trySend(true)
        }
        override fun onLost(network: Network) {
            trySend(checkCurrent())
        }
    }
    cm.registerDefaultNetworkCallback(callback)
    awaitClose { cm.unregisterNetworkCallback(callback) }
}
