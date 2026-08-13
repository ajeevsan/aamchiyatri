const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

const DRIVER_NAMES = [
  "Ganesh Pawar", "Ramesh Yadav", "Iqbal Shaikh", "Suresh Gaikwad",
  "Anthony D'Souza", "Vikram Chavan", "Mohammed Ali", "Sunil More",
  "Rajendra Jadhav", "Prakash Kamble",
];

const VEHICLE_MODELS = {
  AUTO: "Bajaj RE Auto",
  BIKE: "Honda Activa",
  SEDAN: "Suzuki Dzire",
  SUV: "Toyota Innova",
};

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomFloat(min, max) {
  return Math.random() * (max - min) + min;
}

function pick(list) {
  return list[randomInt(0, list.length - 1)];
}

function initials(name) {
  return name.split(" ").map((p) => p[0]).slice(0, 2).join("");
}

/** Nudges a lat/lng point by roughly `km` kilometres in a random direction. */
function offsetPoint(point, km) {
  const degreesPerKm = 1 / 111;
  const angle = randomFloat(0, 2 * Math.PI);
  return {
    lat: point.lat + km * degreesPerKm * Math.cos(angle),
    lng: point.lng + km * degreesPerKm * Math.sin(angle),
  };
}

function lerp(from, to, fraction) {
  return {
    lat: from.lat + (to.lat - from.lat) * fraction,
    lng: from.lng + (to.lng - from.lng) * fraction,
  };
}

/** Re-reads the ride doc; returns its current status, or null if it's gone. */
async function currentStatus(ref) {
  const snap = await ref.get();
  if (!snap.exists) return null;
  return snap.data().status;
}

/**
 * Walks `from` -> `to` over `steps` updates, bailing out early if the rider cancelled
 * (or the ride was otherwise removed) in the meantime.
 */
async function animateBetween(ref, from, to, steps, stepDelayMs) {
  for (let step = 1; step <= steps; step++) {
    const status = await currentStatus(ref);
    if (status === null || status === "CANCELLED") return false;
    const point = lerp(from, to, step / steps);
    await ref.update({ driverLocation: point });
    await sleep(stepDelayMs);
  }
  return true;
}

/**
 * The "backend": this is what a real dispatch/matching service (Beckn search -> select -> init ->
 * confirm -> track, talking to actual driver-partner apps) would do. Running it as a Cloud
 * Function - rather than as a coroutine inside the rider app - is what makes this genuinely a
 * server-side, real-time process: it keeps running even if the rider's app is killed, and every
 * rider app instance watching the same document sees the exact same live updates.
 */
const onRideCreated = onDocumentCreated(
  { document: "rides/{rideId}", region: "asia-south1", timeoutSeconds: 180, memory: "256MiB" },
  async (event) => {
    const ref = event.data.ref;
    const ride = event.data.data();

    // Give a real driver-mode user (DriverRepository.acceptRide) first crack at claiming this
    // ride. If nobody has by the time this fires, fall back to the simulated driver below - this
    // is what keeps a solo/no-drivers-online session fully demoable while still letting a real
    // driver win the race the moment one exists.
    await sleep(randomInt(15000, 20000));
    if ((await currentStatus(ref)) !== "SEARCHING_DRIVER") return;

    const driverFound = Math.random() > 0.08;
    if (!driverFound) {
      await ref.update({ status: "NO_DRIVER_FOUND" });
      return;
    }

    const driverName = pick(DRIVER_NAMES);
    const driver = {
      name: driverName,
      rating: Math.round(randomFloat(4.3, 5.0) * 10) / 10,
      totalTrips: randomInt(120, 9800),
      vehicleNumber: `MH ${String(randomInt(1, 4)).padStart(2, "0")} ` +
        `${String.fromCharCode(65 + randomInt(0, 25))}${String.fromCharCode(65 + randomInt(0, 25))} ${randomInt(1000, 9999)}`,
      vehicleModel: VEHICLE_MODELS[ride.vehicleType] || "Vehicle",
      phoneNumber: `98${randomInt(10000000, 99999999)}`,
      photoInitials: initials(pick(DRIVER_NAMES)),
    };
    const otp = String(randomInt(1000, 9999));
    const pickupPoint = { lat: ride.pickup.lat, lng: ride.pickup.lng };
    const dropPoint = { lat: ride.drop.lat, lng: ride.drop.lng };
    const driverStart = offsetPoint(pickupPoint, randomFloat(1.0, 3.0));

    await ref.update({
      status: "DRIVER_ASSIGNED",
      driver,
      startOtp: otp,
      driverLocation: driverStart,
    });

    await ref.update({ status: "DRIVER_ARRIVING" });
    if (!(await animateBetween(ref, driverStart, pickupPoint, 6, 700))) return;
    if ((await currentStatus(ref)) === "CANCELLED") return;
    await ref.update({ status: "DRIVER_ARRIVED", driverLocation: pickupPoint });

    await sleep(1800); // rider reads the OTP out, driver keys it in
    if ((await currentStatus(ref)) !== "DRIVER_ARRIVED") return;
    await ref.update({ status: "ON_TRIP" });

    if (!(await animateBetween(ref, pickupPoint, dropPoint, 10, 550))) return;
    if ((await currentStatus(ref)) === "CANCELLED") return;

    await ref.update({
      status: "COMPLETED",
      driverLocation: dropPoint,
      completedAt: FieldValue.serverTimestamp(),
      finalFare: ride.fare.totalFare,
    });

    logger.info(`Ride ${event.params.rideId} completed`);
  },
);

module.exports = { onRideCreated };
