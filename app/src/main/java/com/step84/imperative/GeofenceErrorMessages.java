/*
 * Wrapper for geofence error messages.
 */
package com.step84.imperative;

import android.content.Context;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.GeofenceStatusCodes;

/**
 * Wrapper class for error messages related to geofences.
 *
 * @author fredrik.laapotti@gmail.com
 * @version 0.1.181103
 * @since 0.1.181103
 */
public class GeofenceErrorMessages {
    /**
     * Dummy constructor.
     */
    private GeofenceErrorMessages() {}

    /**
     * Returns a stringified error based on exception.
     *
     * @param context App context
     * @param e Geofence exception.
     * @return Stringified error message.
     */
    public static String getErrorString(Context context, Exception e) {
        if (e instanceof ApiException) {
            return getErrorString(context, ((ApiException) e).getStatusCode());
        } else {
            //return context.getResources().getString(R.string.unknown_geofence_error);
            return "unknown geofence error";
        }
    }

    /**
     * Returns a stringified error based on error code.
     *
     * @param context App context.
     * @param errorCode Geofence error code.
     * @return Stringified error message.
     */
    public static String getErrorString(Context context, int errorCode) {
        //Resources mResources = context.getResources();
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                //return mResources.getString(R.string.geofence_not_available);
                return "geofence not available";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                //return mResources.getString(R.string.geofence_too_many_geofences);
                return "too many geofences";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                //return mResources.getString(R.string.geofence_too_many_pending_intents);
                return "too many pending intents";
            default:
                //return mResources.getString(R.string.unknown_geofence_error);
                return "unknown geofence error";
        }
    }
}