package com.step84.imperative;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;

final class Constants {
    private Constants() {
    }

    private static final String TAG = "Constants";
    private static final String PACKAGE_NAME = "com.step84.imperative";
    static final String GEOFENCES_ADDED_KEY = PACKAGE_NAME + ".GEOFENCES_ADDED_KEY";

    private static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;

    static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;
    static final float GEOFENCE_RADIUS_IN_METERS = 100;
    static final HashMap<String, LatLng> ZONES = new HashMap<>();

    static {
        ZONES.put("roundabout", new LatLng(57.488871, 15.841141));
        ZONES.put("parkinglot", new LatLng(57.487119, 15.840213));
    }
}
