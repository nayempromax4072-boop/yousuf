package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

class ChatRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in to chat.")

    fun observeConversation(conversationId: String): Flow<Conversation?> {
        val docRef = db.collection("conversations").document(conversationId)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        Conversation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                if (Firebase.auth.currentUser != null) {
                    handleFirestoreError(fsError ?: error, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    fun observeAllConversations(): Flow<List<Conversation>> {
        val path = "conversations"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        Conversation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }.sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                if (Firebase.auth.currentUser != null) {
                    handleFirestoreError(fsError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeMessages(conversationId: String, limit: Long = 100L): Flow<List<ChatMessage>> {
        val path = "conversations/$conversationId/messages"
        return db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(limit)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                if (Firebase.auth.currentUser != null) {
                    handleFirestoreError(fsError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun sendMessage(
        conversationId: String,
        customerId: String,
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        senderRole: String,
        messageType: String,
        text: String,
        mediaUrl: String = "",
        mediaType: String = "",
        mediaDurationSec: Long = 0L
    ): Result<Unit> {
        val senderId = requireUserId()
        val convRef = db.collection("conversations").document(conversationId)
        val msgRef = convRef.collection("messages").document()

        val previewText = when (messageType) {
            "image" -> if (text.isNotBlank()) text.take(120) else "[Photo]"
            "video" -> if (text.isNotBlank()) text.take(120) else "[Video]"
            "voice" -> "[Voice Message ${mediaDurationSec}s]"
            else -> text.trim().take(200)
        }

        return try {
            val convSnap = convRef.get().await()
            if (!convSnap.exists()) {
                convRef.set(
                    mapOf(
                        "customerId" to customerId,
                        "customerName" to customerName.ifBlank { "Customer" }.take(80),
                        "customerPhone" to customerPhone.take(30),
                        "customerEmail" to customerEmail.take(120),
                        "lastMessageText" to previewText,
                        "lastMessageType" to messageType,
                        "lastSenderRole" to senderRole,
                        "unreadByCustomer" to if (senderRole == "admin") 1L else 0L,
                        "unreadByAdmin" to if (senderRole == "customer") 1L else 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "customerName" to customerName.ifBlank { "Customer" }.take(80),
                    "customerPhone" to customerPhone.take(30),
                    "lastMessageText" to previewText,
                    "lastMessageType" to messageType,
                    "lastSenderRole" to senderRole,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (senderRole == "customer") {
                    updates["unreadByAdmin"] = FieldValue.increment(1L)
                } else {
                    updates["unreadByCustomer"] = FieldValue.increment(1L)
                }
                convRef.update(updates).await()
            }

            msgRef.set(
                mapOf(
                    "conversationId" to conversationId,
                    "customerId" to customerId,
                    "senderId" to senderId,
                    "senderRole" to senderRole,
                    "messageType" to messageType,
                    "text" to text.trim().take(2000),
                    "mediaUrl" to mediaUrl.take(1500),
                    "mediaType" to mediaType.take(100),
                    "mediaDurationSec" to mediaDurationSec.coerceAtLeast(0L),
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val targetUserId = if (senderRole == "customer") "ADMIN" else customerId
            val notifTitle = if (senderRole == "customer") {
                "New message from ${customerName.ifBlank { "Customer" }}"
            } else {
                "Yusuf Electronics Support"
            }
            db.collection("notifications").document().set(
                mapOf(
                    "targetUserId" to targetUserId,
                    "senderId" to senderId,
                    "title" to notifTitle.take(120),
                    "body" to previewText.ifBlank { "New message" }.take(500),
                    "type" to "chat_message",
                    "relatedId" to conversationId,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, msgRef.path)
            Result.failure(e)
        }
    }

    suspend fun markConversationRead(conversationId: String, readerRole: String): Result<Unit> {
        requireUserId()
        val convRef = db.collection("conversations").document(conversationId)
        return try {
            val snap = convRef.get().await()
            if (!snap.exists()) return Result.success(Unit)

            val fieldToReset = if (readerRole == "admin") "unreadByAdmin" else "unreadByCustomer"
            convRef.update(
                mapOf(
                    fieldToReset to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val targetSenderRole = if (readerRole == "admin") "customer" else "admin"
            val unreadMsgs = convRef.collection("messages")
                .whereEqualTo("isRead", false)
                .whereEqualTo("senderRole", targetSenderRole)
                .get()
                .await()

            if (!unreadMsgs.isEmpty) {
                val batch = db.batch()
                unreadMsgs.documents.forEach { doc ->
                    batch.update(
                        doc.reference,
                        mapOf(
                            "isRead" to true,
                            "readAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path)
            Result.failure(e)
        }
    }

    suspend fun uploadBytes(
        storageFolder: String,
        bytes: ByteArray,
        extension: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        return try {
            val fileName = "${UUID.randomUUID()}.$extension"
            val ref = FirebaseStorage.getInstance().reference.child("$storageFolder/$fileName")
            val task = ref.putBytes(bytes)
            task.addOnProgressListener { snap ->
                if (snap.totalByteCount > 0) {
                    onProgress(snap.bytesTransferred.toFloat() / snap.totalByteCount.toFloat())
                }
            }
            task.await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadFile(
        storageFolder: String,
        file: File,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        return try {
            val ext = file.extension.ifBlank { "bin" }
            val fileName = "${UUID.randomUUID()}.$ext"
            val ref = FirebaseStorage.getInstance().reference.child("$storageFolder/$fileName")
            val task = ref.putFile(Uri.fromFile(file))
            task.addOnProgressListener { snap ->
                if (snap.totalByteCount > 0) {
                    onProgress(snap.bytesTransferred.toFloat() / snap.totalByteCount.toFloat())
                }
            }
            task.await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadContentUri(
        storageFolder: String,
        uri: Uri,
        extension: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        return try {
            val fileName = "${UUID.randomUUID()}.$extension"
            val ref = FirebaseStorage.getInstance().reference.child("$storageFolder/$fileName")
            val task = ref.putFile(uri)
            task.addOnProgressListener { snap ->
                if (snap.totalByteCount > 0) {
                    onProgress(snap.bytesTransferred.toFloat() / snap.totalByteCount.toFloat())
                }
            }
            task.await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
