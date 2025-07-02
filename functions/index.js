const admin = require("firebase-admin");
const { onValueWritten } = require("firebase-functions/v2/database");
const { setGlobalOptions } = require("firebase-functions/v2");
const { getDatabase } = require("firebase-admin/database");
const { initializeApp } = require("firebase-admin/app");

admin.initializeApp();
setGlobalOptions({ region: "asia-southeast1" });

/**
 * 1. SOS Alert Notification (DATA-ONLY)
 */
exports.sendSosNotification = onValueWritten(
  "/Riders/{riderId}/liveTracking/sosAlert",
  async (event) => {
    const sos = event.data.after.val();
    const riderId = event.params.riderId;

    if (sos === true) {
      const contactsSnap = await getDatabase().ref(`/Riders/${riderId}/emergencyContacts`).once("value");
      const contacts = contactsSnap.val();
      if (!contacts) return null;

      const locationSnap = await getDatabase().ref(`/Riders/${riderId}/liveTracking/location`).once("value");
      const location = locationSnap.val() || "0,0";
      const googleMapsUrl = `https://maps.google.com/?q=${location}`;

      let riderName = "Your linked rider";
      const riderProfileSnap = await getDatabase().ref(`/Users/${riderId}/name`).once("value");
      if (riderProfileSnap.exists()) riderName = riderProfileSnap.val();

      const tokens = [];
      for (const fid of Object.keys(contacts)) {
        const userSnap = await getDatabase().ref(`/Users/${fid}/fcmToken`).once("value");
        const token = userSnap.val();
        if (token) tokens.push(token);
      }
      if (!tokens.length) return null;

      const multicastMessage = {
        data: {
          title: `SOS: ${riderName}`,
          body: `${riderName} has triggered an SOS alert! Tap to view location.`,
          riderId,
          googleMapsUrl,
          riderName,
          sosType: "SOS",
          userRole: "Family Member"
        },
        tokens: tokens,
      };

      try {
        await admin.messaging().sendEachForMulticast(multicastMessage);
        console.log("✅ SOS sent to:", tokens);
      } catch (err) {
        console.log("❌ Error sending SOS:", err);
      }

      await getDatabase().ref(`/Riders/${riderId}/liveTracking/sosAlert`).set(false);
    }
    return null;
  }
);

/**
 * 2. Trip Start/End Notification (DATA-ONLY)
 */
exports.notifyFamilyOnTripChange = onValueWritten(
  "/Riders/{riderId}/liveTracking/tripActive",
  async (event) => {
    const tripActive = event.data.after.val();
    const prevTripActive = event.data.before.val();
    const riderId = event.params.riderId;

    if (tripActive === prevTripActive) return null;

    let riderName = "Rider";
    const riderProfileSnap = await getDatabase().ref(`/Users/${riderId}/name`).once("value");
    if (riderProfileSnap.exists()) riderName = riderProfileSnap.val();

    const contactsSnap = await getDatabase().ref(`/Riders/${riderId}/emergencyContacts`).once("value");
    const contacts = contactsSnap.val();
    if (!contacts) return null;

    const tokens = [];
    for (const fid of Object.keys(contacts)) {
      const userSnap = await getDatabase().ref(`/Users/${fid}/fcmToken`).once("value");
      const token = userSnap.val();
      if (token) tokens.push(token);
    }
    if (!tokens.length) return null;

    const multicastMessage = {
      data: {
        title: `HelmetHero: Trip ${tripActive ? "Started" : "Ended"}`,
        body: `${riderName} has ${tripActive ? "started" : "ended"} a trip.`,
        riderId,
        tripActive: tripActive ? "1" : "0",
        riderName,
        userRole: "Family Member"
      },
      tokens: tokens
    };

    try {
      await admin.messaging().sendEachForMulticast(multicastMessage);
      console.log(`📤 Trip ${tripActive ? "start" : "end"} alert sent`);
    } catch (err) {
      console.log("❌ Error sending trip notification:", err);
    }
    return null;
  }
);

/**
 * 3. Emergency Contacts Linked/Unlinked Notification (DATA-ONLY)
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

      // Family Notification
      if (familyToken) {
        await admin.messaging().send({
          token: familyToken,
          data: {
            title: "HelmetHero: Linked!",
            body: `${riderName} has linked you as an emergency contact.`,
            riderId,
            linkStatus: "linked",
            riderName,
            userRole: "Family Member"
          }
        });
      }

      // Rider Notification
      if (riderToken) {
        await admin.messaging().send({
          token: riderToken,
          data: {
            title: "HelmetHero: Family Linked",
            body: `You’ve successfully linked with ${familyName}.`,
            familyUid: fid,
            linkStatus: "linked",
            familyName,
            userRole: "Rider"
          }
        });
      }
    }

    for (const fid of removedContacts) {
      const familySnap = await getDatabase().ref(`/Users/${fid}`).once("value");
      const family = familySnap.val();
      const familyName = family?.name || "Family Member";
      const familyToken = family?.fcmToken;

      // Family Notification
      if (familyToken) {
        await admin.messaging().send({
          token: familyToken,
          data: {
            title: "HelmetHero: Unlinked",
            body: `${riderName} has unlinked you from emergency contacts.`,
            riderId,
            linkStatus: "unlinked",
            riderName,
            userRole: "Family Member"
          }
        });
      }

      // Rider Notification
      if (riderToken) {
        await admin.messaging().send({
          token: riderToken,
          data: {
            title: "HelmetHero: Family Unlinked",
            body: `${familyName} has been removed from your emergency contact list.`,
            familyUid: fid,
            linkStatus: "unlinked",
            familyName,
            userRole: "Rider"
          }
        });
      }
    }

    return null;
  }
);
