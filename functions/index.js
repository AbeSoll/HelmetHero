// --- Node.js Cloud Function ---
const admin = require("firebase-admin");
const { onValueWritten } = require("firebase-functions/v2/database");
const { setGlobalOptions } = require("firebase-functions/v2");
const { getDatabase } = require("firebase-admin/database");
const { initializeApp } = require("firebase-admin/app");

admin.initializeApp();
setGlobalOptions({ region: "asia-southeast1" });

/**
 * 1. SOS Alert Notification
 */
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

/**
 * 2. Trip Start/End Notification
 */
exports.notifyFamilyOnTripChange = onValueWritten(
  "/Riders/{riderId}/liveTracking/tripActive",
  async (event) => {
    const tripActive = event.data.after.val();
    const prevTripActive = event.data.before.val();
    const riderId = event.params.riderId;

    // Only send if status changed (from undefined or false to true/false)
    if (tripActive === prevTripActive) return null;

    // Get riderName
    let riderName = "Rider";
    const riderProfileSnap = await getDatabase().ref(`/Users/${riderId}/name`).once("value");
    if (riderProfileSnap.exists()) riderName = riderProfileSnap.val();

    // Get contacts
    const contactsSnap = await getDatabase().ref(`/Riders/${riderId}/emergencyContacts`).once("value");
    const contacts = contactsSnap.val();
    if (!contacts) return null;

    // Get FCM tokens
    const tokens = [];
    for (const fid of Object.keys(contacts)) {
      const userSnap = await getDatabase().ref(`/Users/${fid}/fcmToken`).once("value");
      const token = userSnap.val();
      if (token) tokens.push(token);
    }
    if (!tokens.length) return null;

    // Send notification
    const multicastMessage = {
      notification: {
        title: `HelmetHero: Trip ${tripActive ? "Started" : "Ended"}`,
        body: `${riderName} has ${tripActive ? "started" : "ended"} a trip.`,
      },
      data: {
        riderId,
        tripActive: tripActive ? "1" : "0",
        riderName
      },
      tokens: tokens,
    };
    try {
      await admin.messaging().sendEachForMulticast(multicastMessage);
    } catch (err) {
      console.log("Error sending trip notification:", err);
    }
    return null;
  }
);

/**
 * 3. Emergency Contacts Linked/Unlinked Notification (for both Rider & Family)
 */
exports.notifyFamilyAndRiderOnContactChange = onValueWritten(
  "/Riders/{riderId}/emergencyContacts",
  async (event) => {
    const riderId = event.params.riderId;
    const afterContacts = event.data.after.val() || {};
    const beforeContacts = event.data.before.val() || {};

    const newContacts = Object.keys(afterContacts).filter(x => !beforeContacts.hasOwnProperty(x));
    const removedContacts = Object.keys(beforeContacts).filter(x => !afterContacts.hasOwnProperty(x));

    const riderProfileSnap = await getDatabase().ref(`/Users/${riderId}`).once("value");
    const riderProfile = riderProfileSnap.val();
    const riderName = riderProfile?.name || "Rider";
    const riderToken = riderProfile?.fcmToken || null;

    for (const fid of newContacts) {
      const familySnap = await getDatabase().ref(`/Users/${fid}`).once("value");
      const family = familySnap.val();
      const familyName = family?.name || "Family Member";
      const familyToken = family?.fcmToken;

      // To Family
      if (familyToken) {
        await admin.messaging().send({
          token: familyToken,
          notification: {
            title: "HelmetHero: Linked!",
            body: `${riderName} has linked you as an emergency contact.`
          },
          data: {
            riderId,
            linkStatus: "linked",
            riderName,
            userRole: "Family Member"
          }
        });
        console.log(`✅ Sent link notification to FAMILY: ${familyName}`);
      }

      // To Rider
      if (riderToken) {
        await admin.messaging().send({
          token: riderToken,
          notification: {
            title: "HelmetHero: Family Linked",
            body: `You’ve successfully linked with ${familyName}.`
          },
          data: {
            familyUid: fid,
            linkStatus: "linked",
            familyName,
            userRole: "Rider"
          }
        });
        console.log(`✅ Sent link notification to RIDER: ${riderName}`);
      }
    }

    for (const fid of removedContacts) {
      const familySnap = await getDatabase().ref(`/Users/${fid}`).once("value");
      const family = familySnap.val();
      const familyName = family?.name || "Family Member";
      const familyToken = family?.fcmToken;

      // To Family
      if (familyToken) {
        await admin.messaging().send({
          token: familyToken,
          notification: {
            title: "HelmetHero: Unlinked",
            body: `${riderName} has unlinked you from emergency contacts.`
          },
          data: {
            riderId,
            linkStatus: "unlinked",
            riderName,
            userRole: "Family Member"
          }
        });
        console.log(`📤 Sent unlink notification to FAMILY: ${familyName}`);
      }

      // To Rider
      if (riderToken) {
        await admin.messaging().send({
          token: riderToken,
          notification: {
            title: "HelmetHero: Family Unlinked",
            body: `${familyName} has been removed from your emergency contact list.`
          },
          data: {
            familyUid: fid,
            linkStatus: "unlinked",
            familyName,
            userRole: "Rider"
          }
        });
        console.log(`📤 Sent unlink notification to RIDER: ${riderName}`);
      }
    }

    return null;
  }
);

