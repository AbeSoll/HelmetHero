// --- Node.js Cloud Function ---
const admin = require("firebase-admin");
const { onValueWritten } = require("firebase-functions/v2/database");
const { setGlobalOptions } = require("firebase-functions/v2");
const { getDatabase } = require("firebase-admin/database");
const { initializeApp } = require("firebase-admin/app");

admin.initializeApp();
setGlobalOptions({ region: "asia-southeast1" });

exports.sendSosNotification = onValueWritten(
  "/Riders/{riderId}/liveTracking/sosAlert",
  async (event) => {
    console.log("Function triggered!");
    const sos = event.data.after.val();
    const riderId = event.params.riderId;

    if (sos === true) {
      // Get emergency contacts
      const contactsSnap = await getDatabase().ref(`/Riders/${riderId}/emergencyContacts`).once("value");
      const contacts = contactsSnap.val();
      if (!contacts) return null;

      // Get location
      const locationSnap = await getDatabase().ref(`/Riders/${riderId}/liveTracking/location`).once("value");
      const location = locationSnap.val() || "0,0";
      const googleMapsUrl = `https://maps.google.com/?q=${location}`;

      // Get riderName
      let riderName = "Your linked rider";
      const riderProfileSnap = await getDatabase().ref(`/Users/${riderId}/name`).once("value");
      if (riderProfileSnap.exists()) riderName = riderProfileSnap.val();

      // Get FCM tokens
      const tokens = [];
      for (const fid of Object.keys(contacts)) {
        const userSnap = await getDatabase().ref(`/Users/${fid}/fcmToken`).once("value");
        const token = userSnap.val();
        if (token) tokens.push(token);
      }
      if (!tokens.length) return null;

      // Prepare FCM (notification + data)
      const multicastMessage = {
        notification: {
          title: `SOS: ${riderName}`,
          body: `${riderName} has triggered an SOS alert! Tap to view location.`,
        },
        data: {
          riderId,
          googleMapsUrl,
          riderName,
          sosType: "SOS"
        },
        tokens: tokens,
      };

      try {
        const response = await admin.messaging().sendEachForMulticast(multicastMessage);
        console.log("Notification sent to:", tokens, "Response:", response);
      } catch (err) {
        console.log("Error sending notification:", err);
      }

      await getDatabase().ref(`/Riders/${riderId}/liveTracking/sosAlert`).set(false);
    }
    return null;
  }
);
