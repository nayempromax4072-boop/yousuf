package com.example.data.firebase

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Chunked media storage backed by Cloud Firestore (`media_blobs/{mediaId}/chunks/chunk_{i}`).
 * Used seamlessly as a fallback when Firebase Storage is not initialized on the project,
 * keeping photo, video, and voice attachments working end-to-end across Customer & Admin.
 */
object FirestoreMediaStore {
    const val SCHEME = "fsmedia"
    const val PREFIX = "$SCHEME://"
    const val MAX_MEDIA_BYTES = 10L * 1024L * 1024L // 10 MB
    private const val CHUNK_SIZE_BYTES = 450 * 1024 // ~600 KB Base64 per chunk (< 1 MiB doc limit)

    private val downloadLocks = ConcurrentHashMap<String, Mutex>()

    fun isFsMediaUrl(url: String?): Boolean =
        !url.isNullOrBlank() && url.startsWith(PREFIX)

    fun extractMediaId(url: String): String? =
        if (isFsMediaUrl(url)) {
            url.removePrefix(PREFIX).substringBefore('/').trim().takeIf { it.isNotEmpty() }
        } else {
            null
        }

    fun mimeTypeForExtension(extension: String): String = when (extension.lowercase().trimStart('.')) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "mp4" -> "video/mp4"
        "3gp" -> "video/3gpp"
        "mkv" -> "video/x-matroska"
        "m4a" -> "audio/mp4"
        "mp3" -> "audio/mpeg"
        "aac" -> "audio/aac"
        else -> "application/octet-stream"
    }

    suspend fun readUriBytesBounded(
        context: Context,
        uri: Uri,
        maxBytes: Long = MAX_MEDIA_BYTES
    ): ByteArray = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Unable to read selected file.")
        stream.use { input ->
            val buffer = ByteArray(16 * 1024)
            val out = ByteArrayOutputStream()
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                total += read
                if (total > maxBytes) {
                    val maxMb = (maxBytes / (1024L * 1024L)).coerceAtLeast(1L)
                    throw IllegalArgumentException(
                        "File exceeds $maxMb MB limit. Please capture or choose a smaller file."
                    )
                }
                out.write(buffer, 0, read)
            }
            val result = out.toByteArray()
            if (result.isEmpty()) {
                throw IllegalStateException("Selected file is empty.")
            }
            result
        }
    }

    suspend fun uploadBytes(
        db: FirebaseFirestore,
        ownerId: String,
        recipientId: String = "",
        bytes: ByteArray,
        mimeType: String,
        extension: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        if (bytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("Cannot upload empty file."))
        }
        if (bytes.size.toLong() > MAX_MEDIA_BYTES) {
            return Result.failure(
                IllegalArgumentException("File exceeds 10 MB limit. Please capture a shorter video or smaller file.")
            )
        }

        val cleanExt = extension.lowercase().filter { it.isLetterOrDigit() }.ifBlank { "bin" }.take(16)
        val mediaId = UUID.randomUUID().toString()
        val blobRef = db.collection("media_blobs").document(mediaId)
        val chunksCol = blobRef.collection("chunks")

        val totalChunks = ((bytes.size + CHUNK_SIZE_BYTES - 1) / CHUNK_SIZE_BYTES).coerceAtLeast(1)

        return try {
            onProgress(0.05f)
            for (index in 0 until totalChunks) {
                val start = index * CHUNK_SIZE_BYTES
                val end = minOf(start + CHUNK_SIZE_BYTES, bytes.size)
                val slice = bytes.copyOfRange(start, end)
                val encoded = Base64.encodeToString(slice, Base64.NO_WRAP)

                chunksCol.document("chunk_$index").set(
                    mapOf(
                        "ownerId" to ownerId,
                        "recipientId" to recipientId,
                        "index" to index,
                        "data" to encoded
                    )
                ).await()

                onProgress(0.05f + 0.85f * ((index + 1).toFloat() / totalChunks.toFloat()))
            }

            blobRef.set(
                mapOf(
                    "ownerId" to ownerId,
                    "recipientId" to recipientId,
                    "mimeType" to mimeType.ifBlank { mimeTypeForExtension(cleanExt) }.take(100),
                    "extension" to cleanExt,
                    "byteSize" to bytes.size,
                    "chunkCount" to totalChunks,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            onProgress(1f)
            Result.success("$PREFIX$mediaId")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, blobRef.path)
            Result.failure(e)
        }
    }

    suspend fun resolveToCachedFile(
        context: Context,
        db: FirebaseFirestore,
        mediaUrl: String
    ): File? = withContext(Dispatchers.IO) {
        val mediaId = extractMediaId(mediaUrl) ?: return@withContext null
        val cacheDir = File(context.cacheDir, "fsmedia").apply { mkdirs() }

        findExistingCacheFile(cacheDir, mediaId)?.let { return@withContext it }

        val lock = downloadLocks.getOrPut(mediaId) { Mutex() }
        lock.withLock {
            findExistingCacheFile(cacheDir, mediaId)?.let { return@withLock it }

            val blobRef = db.collection("media_blobs").document(mediaId)
            val metaSnap = try {
                blobRef.get().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, blobRef.path)
                return@withLock null
            }

            if (!metaSnap.exists()) return@withLock null

            val chunkCount = (metaSnap.getLong("chunkCount") ?: 0L).toInt()
            if (chunkCount <= 0) return@withLock null

            val ext = (metaSnap.getString("extension") ?: "bin")
                .lowercase()
                .filter { it.isLetterOrDigit() }
                .ifBlank { "bin" }
                .take(16)

            val tempFile = File(cacheDir, "$mediaId.$ext.tmp")
            val targetFile = File(cacheDir, "$mediaId.$ext")

            try {
                FileOutputStream(tempFile).use { fos ->
                    // Fetch chunks in small batches of 3 for speed without saturating memory
                    val chunksCol = blobRef.collection("chunks")
                    for (batchStart in 0 until chunkCount step 3) {
                        val batchEnd = minOf(batchStart + 3, chunkCount)
                        val batchChunks = coroutineScope {
                            (batchStart until batchEnd).map { idx ->
                                async {
                                    val chunkSnap = chunksCol.document("chunk_$idx").get().await()
                                    val b64 = chunkSnap.getString("data")
                                        ?: throw IllegalStateException("Missing media chunk $idx")
                                    Base64.decode(b64, Base64.DEFAULT)
                                }
                            }.awaitAll()
                        }
                        for (decoded in batchChunks) {
                            fos.write(decoded)
                        }
                    }
                    fos.flush()
                }
                if (targetFile.exists()) targetFile.delete()
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }
                targetFile
            } catch (e: CancellationException) {
                tempFile.delete()
                throw e
            } catch (e: Exception) {
                tempFile.delete()
                handleFirestoreError(e, OperationType.GET, blobRef.path)
                null
            }
        }
    }

    private fun findExistingCacheFile(cacheDir: File, mediaId: String): File? {
        return cacheDir.listFiles()?.firstOrNull { file ->
            file.isFile &&
                file.name.startsWith("$mediaId.") &&
                !file.name.endsWith(".tmp") &&
                file.length() > 0L
        }
    }
}
