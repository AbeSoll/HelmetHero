package com.example.helmethero.utils;

public class HelmetConnectionManager {

    private static boolean isHelmetConnected = false;

    public static boolean isConnected() {
        return isHelmetConnected;
    }

    public static void setConnected(boolean status) {
        isHelmetConnected = status;
    }
}
