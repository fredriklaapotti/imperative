package com.step84.imperative;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * TODO: continue work on this class to handle incoming messages from FCM.
 * Probably handle with an intent to sound the alarm? - NOPE, do another time
 * Prototype is working serverside, just update names of parameters - update serverside and node
 * Next step would be designing a cleaner database layout for monitoring the alarm flags
 */
public class MessageService extends FirebaseMessagingService implements MediaPlayer.OnCompletionListener {
    private static final String TAG = "MessageService";

    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "firestore: From: " + remoteMessage.getFrom());

        /** TEST: delegate audio and handling to AlarmService via intent
         *
         */

        Intent intent = new Intent();
        intent.setAction("com.step84.Imperative.broadcast.ALARM");
        intent.putExtra("data", "Extra data");

        //LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        //localBroadcastManager.registerReceiver(this);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        // --- END TEST RECEIVER, PICK UP ANOTHER TIME

        if(remoteMessage.getData().size() > 0) {
            Log.d(TAG, "firestore: Data payload: " + remoteMessage.getData());
            Map<String, String> data = remoteMessage.getData();
            if(data.get("type").equals("preset")) {
                Log.i(TAG, "firestore: Remote message contained preset alarm");
                String source = data.get("source");

                final AudioManager audioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                final MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(),R.raw.alarmfile);
                final int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                final int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                mediaPlayer.setOnCompletionListener(MessageService.this); // This method plays alarm twice when enabled from zones fragment
                mediaPlayer.start();

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "Imperative")
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentTitle("Alarm activated from " + source)
                        .setContentText("Run away, little girl!")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
                notificationManagerCompat.notify(1, mBuilder.build());
            }
        }
        //audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);


        if(remoteMessage.getNotification() != null) {
            Log.d(TAG, "firestore: Message notification body: " + remoteMessage.getNotification().getBody());
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

    public void onCompletion(MediaPlayer mp) {

    }

    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }
}