package com.example.helmethero.models;

import java.io.Serializable;

public class Alert implements Serializable {
    private String alertId;           // Primary alert ID (Firebase key)
    private String riderUid;
    private String riderName;
    private String profileImageUrl;
    private String type;
    private String time;
    private String location;
    private String status;
    private boolean seen;

    // New field for formatted display
    private String formattedTime;

    // === Constructors ===
    public Alert() {
        // Default constructor required for Firebase
    }

    public Alert(String alertId, String riderUid, String riderName, String profileImageUrl,
                 String type, String time, String location, String status, boolean seen) {
        this.alertId = alertId;
        this.riderUid = riderUid;
        this.riderName = riderName;
        this.profileImageUrl = profileImageUrl;
        this.type = type;
        this.time = time;
        this.location = location;
        this.status = status;
        this.seen = seen;
    }

    // === Alias for swipe-to-delete logic (treat alertId as id) ===
    public String getId() { return alertId; }
    public void setId(String id) { this.alertId = id; }

    // === Getters and Setters ===
    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getRiderUid() { return riderUid; }
    public void setRiderUid(String riderUid) { this.riderUid = riderUid; }

    public String getRiderName() { return riderName; }
    public void setRiderName(String riderName) { this.riderName = riderName; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isSeen() { return seen; }
    public void setSeen(boolean seen) { this.seen = seen; }

    // === New Getter and Setter for formattedTime ===
    public String getFormattedTime() { return formattedTime; }
    public void setFormattedTime(String formattedTime) { this.formattedTime = formattedTime; }
}
