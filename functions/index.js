const { initializeApp } = require("firebase-admin/app");

initializeApp();

const { onRideCreated } = require("./dispatch");
const { createRazorpayOrder, verifyRazorpayPayment } = require("./payments");

exports.onRideCreated = onRideCreated;
exports.createRazorpayOrder = createRazorpayOrder;
exports.verifyRazorpayPayment = verifyRazorpayPayment;
