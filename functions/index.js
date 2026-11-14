const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");

admin.initializeApp();

const DATABASE_ID = "ai-studio-android-yusufele-c45b829b-f383-41e4-b48d-02b8d1a37bc5";
const ADMIN_EMAIL = "nayempromax4072@gmail.com";

function getDb() {
  return admin.firestore(admin.app(), DATABASE_ID);
}

/**
 * Trusted callable to assign Firebase Auth custom claim `admin: true`
 * strictly for the verified shop administrator email.
 */
exports.syncAdminClaim = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Authentication required.");
  }
  const email = (request.auth.token.email || "").toLowerCase();
  const verified = request.auth.token.email_verified === true;
  if (email !== ADMIN_EMAIL || !verified) {
    throw new HttpsError("permission-denied", "Only the verified shop administrator can receive admin claims.");
  }

  await admin.auth().setCustomUserClaims(request.auth.uid, { admin: true });
  await getDb().collection("admins").doc(request.auth.uid).set({
    email,
    assignedAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  return { status: "admin_claim_assigned" };
});

/**
 * Trigger push notification to Admin when a customer submits a new order.
 */
exports.onOrderCreated = onDocumentCreated(
  { document: "orders/{orderId}", database: DATABASE_ID },
  async (event) => {
    const order = event.data?.data();
    if (!order) return;

    const adminsSnap = await getDb().collection("users").where("role", "==", "admin").get();
    const tokens = adminsSnap.docs.map((d) => d.data().fcmToken).filter(Boolean);
    if (tokens.length === 0) return;

    await admin.messaging().sendEachForMulticast({
      tokens,
      notification: {
        title: `New Service Order: ${order.orderCode}`,
        body: `${order.customerName} requested ${order.categoryName}`,
      },
      data: {
        type: "order_new",
        orderId: event.params.orderId,
      },
    });
  }
);

/**
 * Trigger push notification to Customer when Admin updates order status.
 */
exports.onOrderUpdated = onDocumentUpdated(
  { document: "orders/{orderId}", database: DATABASE_ID },
  async (event) => {
    const before = event.data?.before?.data();
    const after = event.data?.after?.data();
    if (!before || !after || before.status === after.status) return;

    const userDoc = await getDb().collection("users").doc(after.customerId).get();
    const token = userDoc.data()?.fcmToken;
    if (!token) return;

    await admin.messaging().send({
      token,
      notification: {
        title: `Order ${after.orderCode} Updated`,
        body: `Your request status is now: ${after.status}`,
      },
      data: {
        type: "order_status",
        orderId: event.params.orderId,
      },
    });
  }
);

/**
 * Trigger push notification on new chat message.
 */
exports.onMessageCreated = onDocumentCreated(
  { document: "conversations/{conversationId}/messages/{messageId}", database: DATABASE_ID },
  async (event) => {
    const msg = event.data?.data();
    if (!msg) return;

    const preview =
      msg.messageType === "image"
        ? "Sent a photo"
        : msg.messageType === "video"
        ? "Sent a video"
        : msg.messageType === "voice"
        ? "Sent a voice message"
        : msg.text || "New message";

    if (msg.senderRole === "customer") {
      const adminsSnap = await getDb().collection("users").where("role", "==", "admin").get();
      const tokens = adminsSnap.docs.map((d) => d.data().fcmToken).filter(Boolean);
      if (tokens.length > 0) {
        await admin.messaging().sendEachForMulticast({
          tokens,
          notification: { title: "New Customer Message", body: preview },
          data: { type: "chat_message", conversationId: event.params.conversationId },
        });
      }
    } else {
      const userDoc = await getDb().collection("users").doc(msg.customerId).get();
      const token = userDoc.data()?.fcmToken;
      if (token) {
        await admin.messaging().send({
          token,
          notification: { title: "Yusuf Electronics Support", body: preview },
          data: { type: "chat_message", conversationId: event.params.conversationId },
        });
      }
    }
  }
);
