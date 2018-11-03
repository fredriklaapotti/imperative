/*
 * JobIntentService for geofence intents.
 * Currently not used, using IntentService.
 */
package com.step84.imperative;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.JobIntentService;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * JobIntentService for geofence transitions.
 * Currently not used, using IntentService.
 */
public class GeofenceTransitionsJobIntentService extends JobIntentService {
    private static final int JOB_ID = 573;
    private static final String TAG = "GeofenceTransitionsJIS";
    //private static final String CHANNEL_ID = "channel_01";

    /**
     * Wrapper for enqueueWork().
     *
     * @param context App context.
     * @param intent Intent from geofence.
     */
    public static void enqueueWork(Context context, Intent intent) {
        Log.i(TAG, "geofence: enqueueWork()");
        enqueueWork(context, GeofenceTransitionsJobIntentService.class, JOB_ID, intent);
    }

    /**
     * Handle intent.
     *
     * @param intent Intent from enqueueWork()? Research.
     */
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.i(TAG, "geofence: in onHandleWork JobIntentService");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if(geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this, geofencingEvent.getErrorCode());
            Log.i(TAG, "geofence: event has error");
            Log.e(TAG, errorMessage);
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            String geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition, triggeringGeofences);
            Log.i(TAG, geofenceTransitionDetails);
            Log.i(TAG, "geofence: enter or exit");
        } else {
            Log.e(TAG, "geofence transition invalid type");
        }
    }

    /**
     * Returns stringified transition details.
     *
     * @param geofenceTransition Transition type.
     * @param triggeringGeofences List of Geofence objects.
     * @return String with transition details.
     */
    private String getGeofenceTransitionDetails(int geofenceTransition, List<Geofence> triggeringGeofences) {
        Log.i(TAG, "geofence: ENTRY: getGeofenceTransitionDetails");
        String geofenceTransitionString = getTransitionString(geofenceTransition);
        ArrayList<String> triggeringGeofencesIdsList = new ArrayList<>();
        for(Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList);
        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    }

    /**
     * Wrapper for formatting return string.
     *
     * @param transitionType Type of transition.
     * @return Stringified details.
     */
    private String getTransitionString(int transitionType) {
        Log.i(TAG, "geofence: ENTRY: getTransitionString");
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                //return getString(R.string.geofence_transition_entered);
                return "geofence transition entered";
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                //return getString(R.string.geofence_transition_exited);
                return "geofence transition exited";
            default:
                //return getString(R.string.unknown_geofence_transition);
                return "unknown geofence transition";
        }
    }

    /**
     * Binds the service and queues work? Research.
     *
     * @param intent Intent from?
     * @return Research this one.
     */
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        Log.i(TAG, "geofence: onBind()");
        enqueueWork(getApplicationContext(), intent);
        return null;
    }

    /**
     * onCreate for a service? Perhaps JobIntentService does this. Research.
     */
    @Override
    public void onCreate() {
        Log.i(TAG, "geofence: onCreate()");
    }

    /**
     * Research this method and everything else related to JobIntentService.
     *
     * @param intent Intent
     * @param flags Flags
     * @param startId startId
     * @return Return integer.
     */
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
}