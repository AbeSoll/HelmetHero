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
    private String status;
    private String date;
    private Map<String, Object> notes;
    private List<Map<String, Double>> path;

    public Trip() {}

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getDistance() { return distance; }
    public void setDistance(String distance) { this.distance = distance; }

    public String getAvgSpeed() { return avgSpeed; }
    public void setAvgSpeed(String avgSpeed) { this.avgSpeed = avgSpeed; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Map<String, Object> getNotes() { return notes; }
    public void setNotes(Map<String, Object> notes) { this.notes = notes; }

    public List<Map<String, Double>> getPath() { return path; }
    public void setPath(List<Map<String, Double>> path) { this.path = path; }
}