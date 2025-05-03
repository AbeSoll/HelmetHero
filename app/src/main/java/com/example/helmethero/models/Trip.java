package com.example.helmethero.models;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Trip implements Serializable {
    private String tripId;
    private String timestamp;
    private String duration;
    private String distance;
    private String avgSpeed;
    private String notes;
    private String status;
    private List<Map<String, Double>> routePoints;  // ✅ New: List of LatLng as map (lat/lng)

    public Trip() {
        // Required for Firebase
    }

    public Trip(String tripId, String timestamp, String duration, String distance,
                String notes, String status) {
        this.tripId = tripId;
        this.timestamp = timestamp;
        this.duration = duration;
        this.distance = distance;
        this.notes = notes;
        this.status = status;
    }

    // ✅ New Getter & Setter
    public List<Map<String, Double>> getRoutePoints() {
        return routePoints;
    }

    public void setRoutePoints(List<Map<String, Double>> routePoints) {
        this.routePoints = routePoints;
    }

    public String getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(String avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getDistance() { return distance; }
    public void setDistance(String distance) { this.distance = distance; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
