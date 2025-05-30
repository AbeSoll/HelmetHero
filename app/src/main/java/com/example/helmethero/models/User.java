package com.example.helmethero.models;

public class User {
    private String uid;
    private String name;
    private String email;
    private String phone;
    private String profileImageUrl;
    private String role;

    // Required no-argument constructor for Firebase
    public User() {}

    // Full constructor with all fields
    public User(String uid, String name, String email, String phone, String profileImageUrl, String role) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
    }

    // Constructor without role (for emergency contacts / minimal usage)
    public User(String uid, String name, String email, String phone, String profileImageUrl) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.profileImageUrl = profileImageUrl;
    }

    // Constructor without email/phone (fallback, if needed)
    public User(String uid, String name, String profileImageUrl) {
        this.uid = uid;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
}