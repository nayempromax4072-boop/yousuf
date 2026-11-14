const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";
const ADMIN_EMAIL = "nayempromax4072@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTs() {
  return new Date();
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
    admin: true,
  });
}

// 1. Unauthenticated access tests
test("Unauthenticated user: cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("orders").get());
  await assertFails(unauthDb.collection("categories").get());
  await assertFails(unauthDb.collection("conversations").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("business_settings").doc("main").get());
});

// 2. Users & Privilege Escalation / Email Spoofing tests
test("Customer: can create and update own profile as customer, cannot escalate to admin", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  const ts = nowTs();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      fullName: "Alice Rahman",
      email: "alice@example.com",
      phone: "01711000000",
      address: "Dhaka",
      photoUrl: "",
      role: "customer",
      totalOrders: 0,
      completedOrders: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Privilege escalation attempt on update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: nowTs(),
    })
  );

  // Shadow update with ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isVerified: true,
      updatedAt: nowTs(),
    })
  );

  // Valid update of permitted fields
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      phone: "01811000000",
      address: "Mirpur 10, Dhaka",
      updatedAt: nowTs(),
    })
  );
});

test("Customer: cannot create profile with admin role or read another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTs();

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      fullName: "Hacker",
      email: "hacker@example.com",
      role: "admin",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      fullName: "Bob",
      email: "bob@example.com",
      role: "customer",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").get());
});

test("Email Spoofing: unverified admin email is rejected from admin privileges", async () => {
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  const ts = nowTs();
  await assertFails(
    spoofDb.collection("categories").doc("cat_1").set({
      nameEn: "Fan Repair",
      nameBn: "ফ্যান মেরামত",
      descriptionEn: "Ceiling fan service",
      descriptionBn: "সিলিং ফ্যান সার্ভিস",
      iconKey: "fan",
      sortOrder: 1,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

// 3. Orders & Isolation tests
test("Orders: customer can create own Pending order, query own orders, cannot read Bob's order", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = nowTs();

  await assertSucceeds(
    aliceDb.collection("orders").doc("order_a1").set({
      orderCode: "YE-1001",
      customerId: ALICE_UID,
      customerName: "Alice Rahman",
      phone: "01711000000",
      categoryId: "fan",
      categoryName: "Fan Installation & Repair",
      description: "Ceiling fan making noise",
      address: "Mirpur, Dhaka",
      preferredDate: "2026-11-15",
      preferredTime: "10:00 AM",
      attachmentUrls: [],
      status: "Pending",
      adminNotes: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Scoped query succeeds
  await assertSucceeds(
    aliceDb.collection("orders").where("customerId", "==", ALICE_UID).get()
  );

  // Unscoped list fails
  await assertFails(aliceDb.collection("orders").get());

  // Cross-user get fails
  await assertFails(bobDb.collection("orders").doc("order_a1").get());

  // Customer cannot directly accept their own order
  await assertFails(
    aliceDb.collection("orders").doc("order_a1").update({
      status: "Accepted",
      updatedAt: nowTs(),
    })
  );

  // Customer can cancel own Pending order
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_a1").update({
      status: "Cancelled",
      updatedAt: nowTs(),
    })
  );

  // Terminal state lock: customer cannot modify cancelled order
  await assertFails(
    aliceDb.collection("orders").doc("order_a1").update({
      status: "Pending",
      updatedAt: nowTs(),
    })
  );
});

// 4. Conversations & Messages isolation tests
test("Conversations & Messages: strict private 1:1 customer-admin isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const admDb = adminContext().firestore();
  const ts = nowTs();

  await assertSucceeds(
    aliceDb.collection("conversations").doc(ALICE_UID).set({
      customerId: ALICE_UID,
      customerName: "Alice",
      customerPhone: "01711000000",
      customerEmail: "alice@example.com",
      lastMessageText: "Hello",
      lastMessageType: "text",
      lastSenderRole: "customer",
      unreadByCustomer: 0,
      unreadByAdmin: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("conversations").doc(ALICE_UID).collection("messages").doc("msg_1").set({
      conversationId: ALICE_UID,
      customerId: ALICE_UID,
      senderId: ALICE_UID,
      senderRole: "customer",
      messageType: "text",
      text: "Hello, I need help with AC",
      mediaUrl: "",
      mediaType: "",
      mediaDurationSec: 0,
      isRead: false,
      createdAt: ts,
    })
  );

  // Customer cannot spoof senderRole as admin
  await assertFails(
    aliceDb.collection("conversations").doc(ALICE_UID).collection("messages").doc("msg_spoof").set({
      conversationId: ALICE_UID,
      customerId: ALICE_UID,
      senderId: ALICE_UID,
      senderRole: "admin",
      messageType: "text",
      text: "Fake admin",
      mediaUrl: "",
      mediaType: "",
      mediaDurationSec: 0,
      isRead: false,
      createdAt: ts,
    })
  );

  // Bob cannot read Alice's conversation or messages
  await assertFails(bobDb.collection("conversations").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("conversations").doc(ALICE_UID).collection("messages").get());

  // Verified Admin can read & reply
  await assertSucceeds(admDb.collection("conversations").get());
  await assertSucceeds(
    admDb.collection("conversations").doc(ALICE_UID).collection("messages").doc("msg_2").set({
      conversationId: ALICE_UID,
      customerId: ALICE_UID,
      senderId: ADMIN_UID,
      senderRole: "admin",
      messageType: "text",
      text: "We will send a technician today.",
      mediaUrl: "",
      mediaType: "",
      mediaDurationSec: 0,
      isRead: false,
      createdAt: ts,
    })
  );
});

// 5. Admin management of categories, orders, announcements, and business settings
test("Admin: can manage categories, orders, announcements, and business settings; customers cannot", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const admDb = adminContext().firestore();
  const ts = nowTs();

  await assertSucceeds(
    admDb.collection("categories").doc("fan").set({
      nameEn: "Fan Installation & Repair",
      nameBn: "ফ্যান ইনস্টলেশন ও মেরামত",
      descriptionEn: "Ceiling and exhaust fan setup",
      descriptionBn: "সিলিং ও এগজস্ট ফ্যান মেরামত",
      iconKey: "fan",
      estimatedPriceBdt: "৳300 - ৳1,200",
      sortOrder: 1,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("categories").get());
  await assertFails(
    aliceDb.collection("categories").doc("fan").update({
      isActive: false,
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    admDb.collection("business_settings").doc("main").set({
      shopName: "YUSUF ELECTRONICS",
      tagline: "Your Trusted Electrical & Technical Service Partner",
      phone: "+880 1700-000000",
      email: ADMIN_EMAIL,
      address: "Dhaka, Bangladesh",
      description: "Electrical, electronics, plumbing, CCTV & building maintenance.",
      workingHours: "9:00 AM - 9:00 PM (Sat - Thu)",
      isServiceAvailable: true,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("business_settings").doc("main").get());
  await assertFails(
    aliceDb.collection("business_settings").doc("main").update({
      phone: "0000",
      updatedAt: nowTs(),
    })
  );
});

// 6. Media Blobs (chunked photo/video/voice storage) isolation tests
test("Media Blobs: owner and admin can read/write; cross-user blocked; admin recipient allowed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const admDb = adminContext().firestore();
  const ts = nowTs();

  await assertSucceeds(
    aliceDb.collection("media_blobs").doc("blob_1").collection("chunks").doc("chunk_0").set({
      ownerId: ALICE_UID,
      recipientId: "",
      index: 0,
      data: "aGVsbG8=",
    })
  );
  await assertSucceeds(
    aliceDb.collection("media_blobs").doc("blob_1").set({
      ownerId: ALICE_UID,
      recipientId: "",
      mimeType: "image/jpeg",
      extension: "jpg",
      byteSize: 5,
      chunkCount: 1,
      createdAt: ts,
    })
  );

  // Owner & Admin can get blob and chunk; Bob cannot
  await assertSucceeds(aliceDb.collection("media_blobs").doc("blob_1").get());
  await assertSucceeds(admDb.collection("media_blobs").doc("blob_1").get());
  await assertSucceeds(admDb.collection("media_blobs").doc("blob_1").collection("chunks").doc("chunk_0").get());
  await assertFails(bobDb.collection("media_blobs").doc("blob_1").get());
  await assertFails(bobDb.collection("media_blobs").doc("blob_1").collection("chunks").doc("chunk_0").get());

  // Customer cannot assign recipientId to another customer
  await assertFails(
    aliceDb.collection("media_blobs").doc("blob_spoof").set({
      ownerId: ALICE_UID,
      recipientId: BOB_UID,
      mimeType: "image/jpeg",
      extension: "jpg",
      byteSize: 5,
      chunkCount: 1,
      createdAt: ts,
    })
  );

  // Admin can send media to Alice and Alice can read it
  await assertSucceeds(
    admDb.collection("media_blobs").doc("blob_adm").collection("chunks").doc("chunk_0").set({
      ownerId: ADMIN_UID,
      recipientId: ALICE_UID,
      index: 0,
      data: "d29ybGQ=",
    })
  );
  await assertSucceeds(
    admDb.collection("media_blobs").doc("blob_adm").set({
      ownerId: ADMIN_UID,
      recipientId: ALICE_UID,
      mimeType: "audio/mp4",
      extension: "m4a",
      byteSize: 5,
      chunkCount: 1,
      createdAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("media_blobs").doc("blob_adm").get());
  await assertSucceeds(aliceDb.collection("media_blobs").doc("blob_adm").collection("chunks").doc("chunk_0").get());
  await assertFails(bobDb.collection("media_blobs").doc("blob_adm").get());
});
