/*
 * Intent service for geofence intents.
 */
package com.step84.imperative;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Service to handle geofence intents.
 * Used for the moment since I couldn't get the JobIntentService to work.
 *
 * @author fredrik.laapotti@gmail.com
 * @version 0.1.181103
 * @since 0.1.181103
 */
public class GeofenceTransitionsIntentService extends IntentService {
    //private static final int JOB_ID = 573;
    private static final String TAG = GeofenceTransitionsIntentService.class.getSimpleName();
    //private static final String CHANNEL_ID = "channel_01";

    /**
     * Dummy constructor.
     */
    public GeofenceTransitionsIntentService() {
        super(TAG);
        //Log.i(TAG, "geofence: constructor intent service");
    }

    /*
    public static void enqueueWork(Context context, Intent intent) {
        Log.i(TAG, "geofence: enqueueWork");
        enqueueWork(context, GeofenceTransitionsIntentService.class, JOB_ID, intent);
    }
    */

    /**
     * Handles geofence intents.
     *
     * @param intent Geofence intent.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "geofence: in onHandleIntent IntentService");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if(geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this, geofencingEvent.getErrorCode());
            Log.i(TAG, "geofence: event has error");
            Log.e(TAG, errorMessage);
            return;
        }

        Intent broadcastIntent = new Intent("geofence-update");

        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            String geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition, triggeringGeofences);
            Log.i(TAG, geofenceTransitionDetails);
            //Log.i(TAG, "geofence: enter or exit");

            for(Geofence geofence : triggeringGeofences) {
                Log.i(TAG, "geofence: ENTER: " + geofence.toString());
                Log.i(TAG, "geofence: ENTER: sending broadcast message");
                broadcastIntent.putExtra("zone", geofenceTransitionDetails);
                LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, getString(R.string.app_name))
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentTitle("Geofence enter")
                        .setContentText(geofenceTransitionDetails)
                        .setPriority(NotificationCompat.PRIORITY_MAX);
                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
                notificationManagerCompat.notify(1, mBuilder.build());
            }

        } else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            String geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition, triggeringGeofences);
            Log.i(TAG, geofenceTransitionDetails);

            for(Geofence geofence : triggeringGeofences) {
                Log.i(TAG, "geofence: EXIT: " + geofence.toString());
                Log.i(TAG, "geofence: EXIT: sending broadcast message");
                broadcastIntent.putExtra("zone", geofenceTransitionDetails);
                LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, getString(R.string.app_name))
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentTitle("Geofence exit")
                        .setContentText(geofenceTransitionDetails)
                        .setPriority(NotificationCompat.PRIORITY_MAX);
                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
                notificationManagerCompat.notify(1, mBuilder.build());
            }
        } else {
            Log.e(TAG, "geofence transition invalid type");
        }
    }

    /**
     * Returns stringified details about the transition.
     *
     * @param geofenceTransition Transition integer.
     * @param triggeringGeofences List of Geofence objects.
     * @return Stringified transition details.
     */
    private String getGeofenceTransitionDetails(int geofenceTransition, List<Geofence> triggeringGeofences) {
        //Log.i(TAG, "geofence: ENTRY: getGeofenceTransitionDetails");
        String geofenceTransitionString = getTransitionString(geofenceTransition);
        ArrayList<String> triggeringGeofencesIdsList = new ArrayList<>();
        for(Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList);
        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    }

    /**
     * Returns transition string based on type.
     *
     * @param transitionType Type of transition.
     * @return Stringified version of the transition.
     */
    private String getTransitionString(int transitionType) {
        //Log.i(TAG, "geofence: ENTRY: getTransitionString");
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                //return getString(R.string.geofence_transition_entered);
                return "geofence enter";
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                //return getString(R.string.geofence_transition_exited);
                return "geofence exit";
            default:
                //return getString(R.string.unknown_geofence_transition);
                return "unknown geofence transition";
        }
    }
}