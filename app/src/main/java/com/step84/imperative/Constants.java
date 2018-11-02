package com.step84.imperative;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    static ArrayList<Zone> zoneArrayList = new ArrayList<Zone>();
    //static ArrayList<String> subscribedTopics = new ArrayList<String>();

    static {
        //ZONES.put("home", new LatLng(57.671013, 15.860407));
        zoneArrayList.add(new Zone("placeholder", new LatLng(59.374349, 13.513987), 100, true));
    }

    static JSONObject zonesJSON;
}
