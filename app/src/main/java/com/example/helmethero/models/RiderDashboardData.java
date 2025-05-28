package com.example.helmethero.models; // Use your correct package!

public class RiderDashboardData {
    public String riderUid;
    public String name;
    public String profileImageUrl;
    public boolean activeRide;
    public String lastTripSummary;

    public RiderDashboardData() {}

    public RiderDashboardData(String riderUid, String name, String profileImageUrl, boolean activeRide, String lastTripSummary) {
        this.riderUid = riderUid;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.activeRide = activeRide;
        this.lastTripSummary = lastTripSummary;
    }
}
