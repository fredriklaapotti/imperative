package com.step84.imperative;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receiver for geofence intents.
 */
public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceBroadcastReceiver";

    /**
     * Receives intents.
     * For now just forward to the intent service.
     * Not used at the moment, we use the GeofenceTransitionsIntentService instead (not Job..)
     *
     * @param context app context
     * @param intent receiving intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "geofence: in onReceive");
        GeofenceTransitionsJobIntentService.enqueueWork(context, intent);
    }
}
