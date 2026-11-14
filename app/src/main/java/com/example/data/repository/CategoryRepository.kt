package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.DefaultServiceCategories
import com.example.data.model.ServiceCategory
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CategoryRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeCategories(includeInactive: Boolean = false): Flow<List<ServiceCategory>> {
        val path = "categories"
        return db.collection(path)
            .orderBy("sortOrder", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                val items = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        ServiceCategory::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                val resolved = items.ifEmpty { DefaultServiceCategories.all }
                if (includeInactive) resolved else resolved.filter { it.isActive }
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

    suspend fun seedDefaultCategoriesIfEmpty(): Result<Unit> {
        val colRef = db.collection("categories")
        return try {
            val existing = colRef.limit(1).get().await()
            if (existing.isEmpty) {
                val batch = db.batch()
                DefaultServiceCategories.all.forEach { cat ->
                    val docRef = colRef.document(cat.id)
                    batch.set(
                        docRef,
                        mapOf(
                            "nameEn" to cat.nameEn,
                            "nameBn" to cat.nameBn,
                            "descriptionEn" to cat.descriptionEn,
                            "descriptionBn" to cat.descriptionBn,
                            "iconKey" to cat.iconKey,
                            "estimatedPriceBdt" to cat.estimatedPriceBdt,
                            "sortOrder" to cat.sortOrder,
                            "isActive" to cat.isActive,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "categories")
            Result.failure(e)
        }
    }

    suspend fun saveCategory(category: ServiceCategory, isNew: Boolean): Result<Unit> {
        val docId = if (isNew || category.id.isBlank()) {
            db.collection("categories").document().id
        } else {
            category.id
        }
        val docRef = db.collection("categories").document(docId)
        return try {
            if (isNew || category.createdAt == null) {
                docRef.set(
                    mapOf(
                        "nameEn" to category.nameEn.trim().take(80),
                        "nameBn" to category.nameBn.trim().ifBlank { category.nameEn.trim() }.take(120),
                        "descriptionEn" to category.descriptionEn.trim().take(400),
                        "descriptionBn" to category.descriptionBn.trim().ifBlank { category.descriptionEn.trim() }.take(500),
                        "iconKey" to category.iconKey.trim().ifBlank { "other" }.take(40),
                        "estimatedPriceBdt" to category.estimatedPriceBdt.trim().take(60),
                        "sortOrder" to category.sortOrder,
                        "isActive" to category.isActive,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "nameEn" to category.nameEn.trim().take(80),
                        "nameBn" to category.nameBn.trim().ifBlank { category.nameEn.trim() }.take(120),
                        "descriptionEn" to category.descriptionEn.trim().take(400),
                        "descriptionBn" to category.descriptionBn.trim().ifBlank { category.descriptionEn.trim() }.take(500),
                        "iconKey" to category.iconKey.trim().ifBlank { "other" }.take(40),
                        "estimatedPriceBdt" to category.estimatedPriceBdt.trim().take(60),
                        "sortOrder" to category.sortOrder,
                        "isActive" to category.isActive,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleCategoryActive(categoryId: String, isActive: Boolean): Result<Unit> {
        val docRef = db.collection("categories").document(categoryId)
        return try {
            docRef.update(
                mapOf(
                    "isActive" to isActive,
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

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        val docRef = db.collection("categories").document(categoryId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
