package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.OrderStatus
import com.example.data.model.ServiceOrder
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in to manage service orders.")

    fun observeCustomerOrders(customerId: String): Flow<List<ServiceOrder>> {
        val path = "orders"
        return db.collection(path)
            .whereEqualTo("customerId", customerId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        ServiceOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                if (Firebase.auth.currentUser?.uid == customerId) {
                    handleFirestoreError(fsError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeAllOrders(): Flow<List<ServiceOrder>> {
        val path = "orders"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        ServiceOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
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

    suspend fun getOrderById(orderId: String): Result<ServiceOrder?> {
        val docRef = db.collection("orders").document(orderId)
        return try {
            val doc = docRef.get().await()
            val order = doc.toObject(
                ServiceOrder::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(order)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitOrder(
        customerName: String,
        phone: String,
        categoryId: String,
        categoryName: String,
        description: String,
        address: String,
        preferredDate: String,
        preferredTime: String,
        attachmentUrls: List<String> = emptyList(),
        videoUrl: String = ""
    ): Result<ServiceOrder> {
        val uid = requireUserId()
        val docRef = db.collection("orders").document()
        val dateStamp = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
        val orderCode = "YE-$dateStamp-${(1000..9999).random()}"

        val payload = mapOf(
            "orderCode" to orderCode,
            "customerId" to uid,
            "customerName" to customerName.trim().take(80),
            "phone" to phone.trim().take(30),
            "categoryId" to categoryId.trim().take(128),
            "categoryName" to categoryName.trim().take(120),
            "description" to description.trim().take(2000),
            "address" to address.trim().take(300),
            "preferredDate" to preferredDate.trim().take(40),
            "preferredTime" to preferredTime.trim().take(40),
            "attachmentUrls" to attachmentUrls.take(5),
            "videoUrl" to videoUrl.take(1500),
            "estimatedCostBdt" to 0L,
            "status" to OrderStatus.PENDING.value,
            "adminNotes" to "",
            "scheduledDate" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            // Notify admin immediately
            val notifRef = db.collection("notifications").document()
            notifRef.set(
                mapOf(
                    "targetUserId" to "ADMIN",
                    "senderId" to uid,
                    "title" to "New Service Order: $orderCode",
                    "body" to "${customerName.trim()} requested $categoryName",
                    "type" to "order_new",
                    "relatedId" to docRef.id,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val created = ServiceOrder(
                id = docRef.id,
                orderCode = orderCode,
                customerId = uid,
                customerName = customerName.trim(),
                phone = phone.trim(),
                categoryId = categoryId,
                categoryName = categoryName,
                description = description.trim(),
                address = address.trim(),
                preferredDate = preferredDate,
                preferredTime = preferredTime,
                attachmentUrls = attachmentUrls,
                videoUrl = videoUrl,
                status = OrderStatus.PENDING.value
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun cancelPendingOrder(orderId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("orders").document(orderId)
        return try {
            docRef.update(
                mapOf(
                    "status" to OrderStatus.CANCELLED.value,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateOrder(
        orderId: String,
        customerId: String,
        orderCode: String,
        newStatus: OrderStatus,
        adminNotes: String,
        scheduledDate: String,
        estimatedCostBdt: Long
    ): Result<Unit> {
        val adminUid = requireUserId()
        val docRef = db.collection("orders").document(orderId)
        return try {
            docRef.update(
                mapOf(
                    "status" to newStatus.value,
                    "adminNotes" to adminNotes.trim().take(1000),
                    "scheduledDate" to scheduledDate.trim().take(60),
                    "estimatedCostBdt" to estimatedCostBdt.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val notifRef = db.collection("notifications").document()
            notifRef.set(
                mapOf(
                    "targetUserId" to customerId,
                    "senderId" to adminUid,
                    "title" to "Order $orderCode Updated",
                    "body" to "Status changed to ${newStatus.value}" +
                        if (scheduledDate.isNotBlank()) " • Scheduled: $scheduledDate" else "",
                    "type" to "order_status",
                    "relatedId" to orderId,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }
}
