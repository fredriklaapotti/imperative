package com.step84.imperative;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.List;

public class GeofenceTransitionsIntentService extends IntentService {
    private static final int JOB_ID = 573;
    private static final String TAG = "GeofenceTransitionsIS";
    private static final String CHANNEL_ID = "channel_01";

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

    @Override
    protected void onHandleIntent(Intent intent) {
        //Log.i(TAG, "geofence: in onHandleIntent IntentService");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if(geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this, geofencingEvent.getErrorCode());
            Log.i(TAG, "geofence: event has error");
            Log.e(TAG, errorMessage);
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            String geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition, triggeringGeofences);
            //Log.i(TAG, geofenceTransitionDetails);
            //Log.i(TAG, "geofence: enter or exit");

            for(Geofence geofence : triggeringGeofences) {
                Log.i(TAG, "geofence: ENTER: " + geofence.toString());
            }


            /**
             * IMPLEMENT SUBSCRIPTION BASED ON GEOFENCE
             */

            /*
            FirebaseMessaging.getInstance().subscribeToTopic(selected)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()) {
                                editor.putBoolean(sharedPreferences.getString("selectedZone", ""), true).apply();
                                Log.i(TAG, "firestore: successfully subscribed to topic " + sharedPreferences.getString("selectedZone", ""));
                            }
                        }
                    });

            */
            /**
             * END IMPLEMENT SUBSCRIPTION BASED ON GEOFENCE
             */


        } else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            String geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition, triggeringGeofences);

            for(Geofence geofence : triggeringGeofences) {
                Log.i(TAG, "geofence: EXIT: " + geofence.toString());
            }
        } else {
            Log.e(TAG, "geofence transition invalid type");
        }
    }

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

    private String getTransitionString(int transitionType) {
        //Log.i(TAG, "geofence: ENTRY: getTransitionString");
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
}
