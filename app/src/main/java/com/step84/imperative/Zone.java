package com.step84.imperative;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.Map;

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
    private Map<String, Object> settings;

    public Zone(String id, String name, LatLng latlng, double radius, boolean subscribed) {
        this.id = id;
        this.name = name;
        this.latlng = latlng;
        this.radius = radius;
        this.subscribed = subscribed;

        this.settings = new HashMap<>();
        this.settings.put("alarm_override_sound", false);
        this.settings.put("alarm_notice", true);
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

    public void setAlarmOverrideSound(boolean flag) { this.settings.put("alarm_override_sound", flag); }
    public void setAlarmNotice(boolean flag) { this.settings.put("alarm_notice", flag); }

    public void setSettings(Map<String, Object> settings) { this.settings = settings; }
    public Map<String, Object> getSettings() { return this.settings; }
}