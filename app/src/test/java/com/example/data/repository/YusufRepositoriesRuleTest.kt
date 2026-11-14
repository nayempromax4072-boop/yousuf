package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class YusufRepositoriesRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun customerProfile_createAndReadOwnProfile_succeeds() = runBlocking {
        val uid = signInTestUser("customer_${UUID.randomUUID()}@test.com")
        val userRepo = UserRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            userRepo.ensureUserProfile(
                uid = uid,
                email = "customer@test.com",
                displayName = "Rahim Uddin",
                photoUrl = "",
                isAdmin = false
            )
        }
        assertTrue("Profile creation should succeed: ${result.exceptionOrNull()}", result.isSuccess)
        assertEquals("customer", result.getOrThrow().role)
    }

    @Test
    fun serviceOrder_submitAndObserveOwnOrders_succeeds() = runBlocking {
        val uid = signInTestUser("order_${UUID.randomUUID()}@test.com")
        val orderRepo = OrderRepository(firestore)

        val submitResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            orderRepo.submitOrder(
                customerName = "Karim Mia",
                phone = "01712345678",
                categoryId = "cat_fan",
                categoryName = "Fan Installation & Repair",
                description = "Ceiling fan capacitor replacement needed",
                address = "Dhanmondi 27, Dhaka",
                preferredDate = "2026-11-20",
                preferredTime = "11:00 AM"
            )
        }
        assertTrue("Order submit should succeed: ${submitResult.exceptionOrNull()}", submitResult.isSuccess)
        val createdOrder = submitResult.getOrThrow()
        assertTrue(createdOrder.orderCode.startsWith("YE-"))

        val orders = withTimeout(FLOW_TIMEOUT_MS) {
            orderRepo.observeCustomerOrders(uid).first { list -> list.any { it.id == createdOrder.id } }
        }
        assertTrue(orders.any { it.id == createdOrder.id })
    }

    @Test
    fun serviceOrder_crossUserRead_failsWithPermissionDenied() = runBlocking {
        signInTestUser("alice_${UUID.randomUUID()}@test.com")
        val aliceOrders = OrderRepository(firestore)
        val created = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceOrders.submitOrder(
                customerName = "Alice",
                phone = "01711111111",
                categoryId = "cat_ac",
                categoryName = "AC Installation & Servicing",
                description = "AC cooling low",
                address = "Uttara, Dhaka",
                preferredDate = "2026-11-21",
                preferredTime = "03:00 PM"
            ).getOrThrow()
        }

        signInTestUser("bob_${UUID.randomUUID()}@test.com")
        val bobOrders = OrderRepository(firestore)
        val bobRead = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobOrders.getOrderById(created.id)
        }
        assertTrue("Cross-user read must fail", bobRead.isFailure)
        val fsEx = bobRead.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun chatMessage_customerSendAndObserveOwnConversation_succeeds() = runBlocking {
        val uid = signInTestUser("chat_${UUID.randomUUID()}@test.com")
        val chatRepo = ChatRepository(firestore)

        val sendResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            chatRepo.sendMessage(
                conversationId = uid,
                customerId = uid,
                customerName = "Hasan Ali",
                customerPhone = "01812345678",
                customerEmail = "hasan@test.com",
                senderRole = "customer",
                messageType = "text",
                text = "Assalamu Alaikum, need an electrician today."
            )
        }
        assertTrue("Sending chat message should succeed: ${sendResult.exceptionOrNull()}", sendResult.isSuccess)

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            chatRepo.observeMessages(uid).first { it.isNotEmpty() }
        }
        assertEquals(1, messages.size)
        assertEquals("customer", messages.first().senderRole)
    }

    @Test
    fun observeCustomerOrders_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val orderRepo = OrderRepository(firestore)

        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                orderRepo.observeCustomerOrders("any_uid").first()
            }
        } catch (e: Exception) {
            caught = e
        }
        assertNotNull("Expected PERMISSION_DENIED exception when unauthenticated", caught)
        val fsEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
