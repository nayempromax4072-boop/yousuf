package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val lastMessageText: String = "",
    val lastMessageType: String = "text",
    val lastSenderRole: String = "customer",
    val unreadByCustomer: Long = 0L,
    val unreadByAdmin: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val customerId: String = "",
    val senderId: String = "",
    val senderRole: String = "customer",
    val messageType: String = "text", // text, image, video, voice
    val text: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "",
    val mediaDurationSec: Long = 0L,
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false,
    val readAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)
