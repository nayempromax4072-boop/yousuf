package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val targetUserId: String = "",
    val senderId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "order_status",
    val relatedId: String = "",
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BusinessSettings(
    @DocumentId val id: String = "",
    val shopName: String = "YUSUF ELECTRONICS",
    val tagline: String = "Your Trusted Electrical & Technical Service Partner",
    val phone: String = "+880 1700-000000",
    val email: String = "nayempromax4072@gmail.com",
    val address: String = "Dhaka, Bangladesh",
    val description: String = "Official customer service and work-order center for electrical, electronics, AC, motor, CCTV, plumbing, and building maintenance.",
    val workingHours: String = "9:00 AM - 9:00 PM (Sat - Thu)",
    @get:PropertyName("isServiceAvailable") @set:PropertyName("isServiceAvailable")
    var isServiceAvailable: Boolean = true,
    val logoUrl: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)
