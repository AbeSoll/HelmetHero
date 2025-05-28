package com.example.helmethero.models;

public class User {
    private String uid;
    private String name;
    private String phone;
    private String email;
    private String profileImageUrl;
    private String role; // (optional, for filtering/searching in Firebase)

    // Required no-argument constructor for Firebase
    public User() {}

    // Full constructor (all fields)
    public User(String uid, String name, String email, String phone, String profileImageUrl, String role) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
    }

    // Short constructor (for quick display when not all info needed)
    public User(String uid, String name, String profileImageUrl) {
        this.uid = uid;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    public User(String uid, String name, String email, String phone, String image) {
    }

    // Getters & Setters
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

    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
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
