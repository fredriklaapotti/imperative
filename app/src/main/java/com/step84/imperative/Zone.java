package com.step84.imperative;

import com.google.android.gms.maps.model.LatLng;

/**
 * Local zone object to mirror the ones from database.
 * Implemented as an ArrayList<Zone> zoneArrayList in Constants.zoneArrayList
 */
public class Zone {
    private final String id;
    private final String name;
    private final LatLng latlng;
    private final double radius;
    private boolean subscribed;

    public Zone(String id, String name, LatLng latlng, double radius, boolean subscribed) {
        this.id = id;
        this.name = name;
        this.latlng = latlng;
        this.radius = radius;
        this.subscribed = subscribed;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public LatLng getLatlng() {
        return this.latlng;
    }

    public double getLat() { return this.latlng.latitude; }
    public double getLng() { return this.latlng.longitude; }

    public double getRadius() {
        return this.radius;
    }

    public boolean getSubscribed() { return this.subscribed; }
    public void setSubscribed(boolean flag) { this.subscribed = flag; }
}