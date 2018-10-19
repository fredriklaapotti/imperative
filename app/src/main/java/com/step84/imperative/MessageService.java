package com.step84.imperative;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * TODO: continue work on this class to handle incoming messages from FCM.
 * Probably handle with an intent to sound the alarm?
 * Prototype is working serverside, just update names of parameters
 * Next step would be designing a cleaner database layout for monitoring the alarm flags
 */
public class MessageService extends FirebaseMessagingService {
    private static final String TAG = "MessageService";

    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if(remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Data payload: " + remoteMessage.getData());
        }

        if(remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message notification body: " + remoteMessage.getNotification().getBody());
        }
    }

    // What did I use below for? This is handled in the loop in home fragment
    /*
    @Override
    public void onNewToken(String token) {
        Log.d("Messaging", "Refreshed token:" + token);
        //sendRegistrationToServer(token);
    }
    */
}