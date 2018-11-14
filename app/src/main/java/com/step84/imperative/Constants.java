/*
 * App wide class for keeping objects in memory.
 */
package com.step84.imperative;

import android.location.Location;

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
    public static String BROADCAST_GEOFENCEUPDATE = "broadcast-geofence";
    public static String BROADCAST_GEOFENCEPUDATE_ZONE = "zone";

    public static String DATABASE_COLLECTION_USERS = "users";
    public static String DATABASE_COLLECTION_USERS_CREATED = "created";
    public static String DATABASE_COLLECTION_USERS_FIELD_LASTUPDATED = "lastUpdated";
    public static String DATABASE_COLLECTION_USERS_FIELD_EMAIL = "email";

    public static String DATABASE_COLLECTION_SUBSCRIPTIONS = "subscriptions";
    public static String DATABASE_COLLECTION_SUBSCRIPTIONS_ACTIVE = "active";
    public static String DATABASE_COLLECTION_SUBSCRIPTIONS_USER = "user";
    public static String DATABASE_COLLECTION_SUBSCRIPTIONS_ZONE = "zone";

    public static String DATABASE_COLLECTION_ZONES = "zones";
    public static String DATABASE_COLLECTION_ZONES_ADDED = "added";
    public static String DATABASE_COLLECTION_ZONES_LATLNG = "latlng";
    public static String DATABASE_COLLECTION_ZONES_NAME = "name";
    public static String DATABASE_COLLECTION_ZONES_RADIUS = "radius";



    static final String GEOFENCES_ADDED_KEY = PACKAGE_NAME + ".GEOFENCES_ADDED_KEY";

    private static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;

    static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;
    static final ArrayList<Zone> zoneArrayList = new ArrayList<>();

    public static Location currentLocation;

    // TODO: this is necessary at the moment to populate geofences at startup - but it's ugly.
    static {
        zoneArrayList.add(new Zone("X", "placeholder", new LatLng(59.374349, 13.513987), 100, true));
    }
}
