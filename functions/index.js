const { initializeApp } = require("firebase-admin/app");

initializeApp();

const { onRideCreated } = require("./dispatch");

exports.onRideCreated = onRideCreated;
