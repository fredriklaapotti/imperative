/*
 * App wide class for keeping objects in memory.
 */
package com.step84.imperative;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * App wide class for handling in-app in-memory constants and variables.
 *
 * @author fredrik.laapotti@gmail.com
 * @version 0.1.181103
 * @since 0.1.181103
 */
final class Constants {
    /**
     * Dummy constructor.
     */
    private Constants() {
    }

    private static final String PACKAGE_NAME = "com.step84.imperative";
    public static final String SP_EMAIL = "email";
    public static final String SP_SELECTEDZONE = "selectedZone";
    public static final String SP_SPINNERSELECTED = "userChoiceSpinner";
    public static boolean REQUESTINGLOCATIONUPDATES = false;

    static final String GEOFENCES_ADDED_KEY = PACKAGE_NAME + ".GEOFENCES_ADDED_KEY";

    private static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;

    static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;
    static final ArrayList<Zone> zoneArrayList = new ArrayList<>();

    // TODO: this is necessary at the moment to populate geofences at startup - but it's ugly.
    static {
        zoneArrayList.add(new Zone("placeholder", new LatLng(59.374349, 13.513987), 100, true));
    }
}
