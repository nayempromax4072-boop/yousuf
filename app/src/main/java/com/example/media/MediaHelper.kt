package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

object MediaHelper {
    const val MAX_VIDEO_BYTES = 20L * 1024L * 1024L // 20 MB

    suspend fun compressImageToJpeg(context: Context, uri: Uri, maxDim: Int = 1080, quality: Int = 78): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val original = inputStream.use { BitmapFactory.decodeStream(it) } ?: return@withContext null

                val width = original.width
                val height = original.height
                val scale = if (width > maxDim || height > maxDim) {
                    minOf(maxDim.toFloat() / width, maxDim.toFloat() / height)
                } else {
                    1f
                }

                val targetBitmap = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        original,
                        (width * scale).toInt().coerceAtLeast(1),
                        (height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                } else {
                    original
                }

                val out = ByteArrayOutputStream()
                targetBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                if (targetBitmap !== original) targetBitmap.recycle()
                original.recycle()
                out.toByteArray()
            } catch (_: Exception) {
                null
            }
        }

    fun getFileSizeBytes(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0) cursor.getLong(idx) else -1L
                } else -1L
            } ?: -1L
        } catch (_: Exception) {
            -1L
        }
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
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(22050)
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
        try {
            val mp = MediaPlayer().apply {
                setDataSource(source)
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
