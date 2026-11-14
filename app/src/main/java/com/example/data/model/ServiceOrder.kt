package com.example.data.model

import com.example.localization.AppLanguage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class OrderStatus(
    val value: String,
    val labelEn: String,
    val labelBn: String
) {
    PENDING("Pending", "Pending", "পেন্ডিং"),
    ACCEPTED("Accepted", "Accepted", "গৃহীত"),
    CONTACTED("Contacted", "Contacted", "যোগাযোগ করা হয়েছে"),
    WORK_SCHEDULED("Work Scheduled", "Work Scheduled", "সময় নির্ধারিত"),
    IN_PROGRESS("In Progress", "In Progress", "কাজ চলছে"),
    COMPLETED("Completed", "Completed", "সম্পন্ন"),
    CANCELLED("Cancelled", "Cancelled", "বাতিল");

    fun localized(language: AppLanguage): String =
        if (language == AppLanguage.BENGALI) labelBn else labelEn

    companion object {
        fun fromValue(value: String): OrderStatus =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: PENDING
    }
}

@IgnoreExtraProperties
data class ServiceOrder(
    @DocumentId val id: String = "",
    val orderCode: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val phone: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val description: String = "",
    val address: String = "",
    val preferredDate: String = "",
    val preferredTime: String = "",
    val attachmentUrls: List<String> = emptyList(),
    val videoUrl: String = "",
    val estimatedCostBdt: Long = 0L,
    val status: String = OrderStatus.PENDING.value,
    val adminNotes: String = "",
    val scheduledDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val statusEnum: OrderStatus
        get() = OrderStatus.fromValue(status)
}
