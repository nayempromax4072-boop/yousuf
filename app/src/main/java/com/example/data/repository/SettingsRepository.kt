package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.BusinessSettings
import com.google.firebase.Firebase
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

class SettingsRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeBusinessSettings(): Flow<BusinessSettings> {
        val docRef = db.collection("business_settings").document("main")
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        BusinessSettings::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: BusinessSettings()
                } else {
                    BusinessSettings()
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

    suspend fun saveBusinessSettings(settings: BusinessSettings): Result<Unit> {
        val docRef = db.collection("business_settings").document("main")
        return try {
            docRef.set(
                mapOf(
                    "shopName" to settings.shopName.trim().ifBlank { "YUSUF ELECTRONICS" }.take(80),
                    "tagline" to settings.tagline.trim().ifBlank { "Your Trusted Electrical & Technical Service Partner" }.take(160),
                    "phone" to settings.phone.trim().ifBlank { "+880 1700-000000" }.take(30),
                    "email" to settings.email.trim().ifBlank { "nayempromax4072@gmail.com" }.take(120),
                    "address" to settings.address.trim().ifBlank { "Dhaka, Bangladesh" }.take(300),
                    "description" to settings.description.trim().ifBlank { "Official technical services." }.take(1000),
                    "workingHours" to settings.workingHours.trim().ifBlank { "9:00 AM - 9:00 PM" }.take(160),
                    "isServiceAvailable" to settings.isServiceAvailable,
                    "logoUrl" to settings.logoUrl.take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }
}
