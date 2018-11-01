package com.step84.imperative;

import com.google.android.gms.maps.model.LatLng;

/**
 * TODO: perhaps use this class? What are the alternatives?
 * Store JSON in SharedPrefs?
 * Store a bunch of these objects in SharedPrefs with Gson?
 */
public class Zone {
    private String name;
    private LatLng latlng;
    private double radius;

    public Zone(String name, LatLng latlng, double radius) {
        this.name = name;
        this.latlng = latlng;
        this.radius = radius;
    }

    public String getName() {
        return this.name;
    }

    public LatLng getLatlng() {
        return this.latlng;
    }

    public double getRadius() {
        return this.radius;
    }
}
