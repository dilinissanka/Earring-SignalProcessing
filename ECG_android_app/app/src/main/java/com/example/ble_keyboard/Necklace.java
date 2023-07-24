package com.example.ble_keyboard;

public class Necklace {
    public String name;
    public boolean connectionStatus;
    public String heartRate;

    public Necklace(String name, boolean connectionStatus, String heartRate) {
        this.name = name;
        this.connectionStatus = connectionStatus;
        this.heartRate = heartRate;
    }

    public String getName() {
        return this.name;
    }

    public boolean getConnectionStatus() {
        return this.connectionStatus;
    }

    public String getHeartRate() {
        return this.heartRate;
    }


    public String toString() {
        return "(" + this.name + ", " + this.connectionStatus + ", " + this.heartRate + ")";
    }


}
