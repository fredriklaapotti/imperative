package com.step84.imperative;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

/**
 * TODO: perhaps use this class? What are the alternatives?
 * Store JSON in SharedPrefs?
 * Store a bunch of these objects in SharedPrefs with Gson?
 */
public class Zone {
    private String name;
    private LatLng latlng;
    private double radius;
    private boolean subscribed;

    public Zone(String name, LatLng latlng, double radius, boolean subscribed) {
        this.name = name;
        this.latlng = latlng;
        this.radius = radius;
        this.subscribed = subscribed;
    }

    public String getName() {
        return this.name;
    }

    public LatLng getLatlng() {
        return this.latlng;
    }

    public double getLat() { return this.latlng.latitude; };
    public double getLng() { return this.latlng.longitude; };

    public double getRadius() {
        return this.radius;
    }

    public boolean getSubscribed() { return this.subscribed; };
    public void setSubscribed(boolean flag) { this.subscribed = flag; }
}
