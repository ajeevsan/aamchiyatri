const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const crypto = require("crypto");
const Razorpay = require("razorpay");

// Set with: firebase functions:secrets:set RAZORPAY_KEY_SECRET
const razorpayKeySecret = defineSecret("RAZORPAY_KEY_SECRET");
// Public key id - not sensitive, but still kept out of source in functions/.env (see .env.example).
const RAZORPAY_KEY_ID = process.env.RAZORPAY_KEY_ID;

function razorpayClient(secret) {
  return new Razorpay({ key_id: RAZORPAY_KEY_ID, key_secret: secret });
}

/**
 * Creates a Razorpay order server-side, where the secret key can actually live safely. The app
 * calls this, then opens Razorpay's Android Checkout with the returned order id.
 */
const createRazorpayOrder = onCall(
  { region: "asia-south1", secrets: [razorpayKeySecret] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Sign in before starting a payment.");
    }
    const rideId = request.data?.rideId;
    const amountRupees = Number(request.data?.amountRupees);
    if (!rideId || !Number.isFinite(amountRupees) || amountRupees <= 0) {
      throw new HttpsError("invalid-argument", "rideId and a positive amountRupees are required.");
    }

    const rideRef = getFirestore().collection("rides").doc(rideId);
    const rideSnap = await rideRef.get();
    if (!rideSnap.exists || rideSnap.data().riderId !== request.auth.uid) {
      throw new HttpsError("permission-denied", "This ride does not belong to you.");
    }

    const razorpay = razorpayClient(razorpayKeySecret.value());
    const order = await razorpay.orders.create({
      amount: Math.round(amountRupees * 100), // paise
      currency: "INR",
      receipt: rideId,
      notes: { rideId, riderId: request.auth.uid },
    });

    await rideRef.update({ razorpayOrderId: order.id, paymentStatus: "PENDING" });

    return { orderId: order.id, amountPaise: order.amount, currency: order.currency, keyId: RAZORPAY_KEY_ID };
  },
);

/**
 * Verifies the payment signature Razorpay Checkout hands back to the app after a successful
 * charge. This MUST happen server-side - the whole point is that only someone holding the secret
 * key (never shipped in the app) can produce a signature that matches.
 */
const verifyRazorpayPayment = onCall(
  { region: "asia-south1", secrets: [razorpayKeySecret] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Sign in before verifying a payment.");
    }
    const { rideId, razorpayOrderId, razorpayPaymentId, razorpaySignature } = request.data || {};
    if (!rideId || !razorpayOrderId || !razorpayPaymentId || !razorpaySignature) {
      throw new HttpsError("invalid-argument", "rideId, razorpayOrderId, razorpayPaymentId and razorpaySignature are required.");
    }

    const expectedSignature = crypto
      .createHmac("sha256", razorpayKeySecret.value())
      .update(`${razorpayOrderId}|${razorpayPaymentId}`)
      .digest("hex");

    const verified = expectedSignature === razorpaySignature;

    const rideRef = getFirestore().collection("rides").doc(rideId);
    const rideSnap = await rideRef.get();
    if (!rideSnap.exists || rideSnap.data().riderId !== request.auth.uid) {
      throw new HttpsError("permission-denied", "This ride does not belong to you.");
    }

    await rideRef.update({
      paymentStatus: verified ? "PAID" : "FAILED",
      razorpayPaymentId,
      paidAt: verified ? FieldValue.serverTimestamp() : null,
    });

    if (!verified) {
      throw new HttpsError("failed-precondition", "Payment signature did not verify.");
    }
    return { verified: true };
  },
);

module.exports = { createRazorpayOrder, verifyRazorpayPayment };
