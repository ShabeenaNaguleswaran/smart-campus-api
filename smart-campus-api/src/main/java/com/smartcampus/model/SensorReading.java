package com.smartcampus.model;

import java.util.UUID;

public class SensorReading {

    private String id;
    private long timestamp;
    private double value;

    // Default constructor (required for JSON)
    public SensorReading() {}

    // Constructor — auto-generates ID and timestamp
    public SensorReading(double value) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.value = value;
    }

    // Getters
    public String getId() { return id; }
    public long getTimestamp() { return timestamp; }
    public double getValue() { return value; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setValue(double value) { this.value = value; }
}