/*
 * Message service for Firebase Cloud Messaging.
 */
package com.step84.imperative;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Handle messages from Firebase Cloud Messaging.
 *
 * @author fredrik.laapotti@gmail.com
 * @version 0.1.181103
 * @since 0.1.181103
 */
public class MessageService extends FirebaseMessagingService implements MediaPlayer.OnCompletionListener {
    private static final String TAG = MessageService.class.getSimpleName();

    //FirebaseStorage storage = FirebaseStorage.getInstance();
    //private StorageReference storageReference;

    /**
     * Receive and handle messages.
     *
     * @param remoteMessage Incoming message.
     */
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "firestore: From: " + remoteMessage.getFrom());

        Intent intent = new Intent();
        intent.setAction("com.step84.Imperative.broadcast.ALARM");
        intent.putExtra("data", "Extra data");

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        if(remoteMessage.getData().size() > 0) {
            Log.d(TAG, "firestore: Data payload: " + remoteMessage.getData());
            Map<String, String> data = remoteMessage.getData();
            Log.d(TAG, "firestore: alarm timestamp from server = " + data.get("activated"));
            String dateString = data.get("activated");
            DateFormat format = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss", Locale.ENGLISH);

            if(dateString != null) {
                try {
                    Date date = new Date();
                    date = format.parse(dateString);
                    Log.i(TAG, "firestore: alarm timestamp from Date.. = " + date.toString());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            // Preset message received
            AudioManager audioManager;
            if(data.get("type").equals("preset")) {
                Log.i(TAG, "firestore: Remote message contained preset alarm");
                String source = data.get("source");

                audioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.alarmfile);
                //int currentVolume = Objects.requireNonNull(audioManager).getStreamVolume(AudioManager.STREAM_MUSIC);
                int maxVolume = Objects.requireNonNull(audioManager).getStreamMaxVolume(AudioManager.STREAM_MUSIC);

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

            // Recorded message received
            if(data.get("type").equals("record")) {
                Log.i(TAG, "firestore: Remote message contained record");
                String source = data.get("source");
                String url = data.get("alarmfile");
                Log.i(TAG, "firestore: alarmfile url = " + url);

                audioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                MediaPlayer alarmPlayer = new MediaPlayer();

                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();

                alarmPlayer.setAudioAttributes(audioAttributes);

                try {
                    alarmPlayer.setDataSource(url);
                } catch(IllegalArgumentException iae) {
                    Log.d(TAG,"firestore: error iae");
                } catch(IOException ioe) {
                    Log.d(TAG,"firestore: error ioe");
                }

                try {
                    alarmPlayer.prepare();
                } catch(IOException ioe) {
                    Log.d(TAG, "firestore: error ioe 2");
                }

                //int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                int maxVolume = Objects.requireNonNull(audioManager).getStreamMaxVolume(AudioManager.STREAM_MUSIC);

                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                alarmPlayer.setOnCompletionListener(MessageService.this); // This method plays alarm twice when enabled from zones fragment
                alarmPlayer.start();
                // audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE); // When should we implement this?

                /*
                 * THIS METHOD BELOWS SAVES TO LOCAL FILE - OR SHOULD
                 * Haven't implemented to play from local file, but download etc works
                 * Just change alarmPlayer method to play locally I suppose
                 */
                /*
                storageReference = storage.getReferenceFromUrl(url);

                try {
                    localFile = File.createTempFile("alarm", "3gp");
                    Log.i(TAG, "firestore: localFile created for temporary use");
                } catch (IOException ioe) {
                    Log.d(TAG, "firestore: failed to create temporary file");
                }

                storageReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        Log.i(TAG, "firestore: downloaded to local file");
                        // File has been created

                        audioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                        //mediaPlayer = MediaPlayer.create(getApplicationContext(),R.raw.alarmfile);
                        MediaPlayer alarmPlayer = new MediaPlayer();
                        alarmPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        try {
                            alarmPlayer.setDataSource(url);
                        } catch(IllegalArgumentException iae) {
                            Log.d(TAG,"firestore: error iae");
                        } catch(IOException ioe) {
                            Log.d(TAG,"firestore: error ioe");
                        }
                        try {
                            alarmPlayer.prepare();
                        } catch(IOException ioe) {
                            Log.d(TAG, "firestore: error ioe 2");
                        }

                        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                        alarmPlayer.setOnCompletionListener(MessageService.this); // This method plays alarm twice when enabled from zones fragment
                        alarmPlayer.start();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG,"firestore: could not download file");
                    }
                });
                */

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, getString(R.string.app_name))
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentTitle("Alarm activated from " + source)
                        .setContentText("Run away, little girl!")
                        .setPriority(NotificationCompat.PRIORITY_MAX);
                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
                notificationManagerCompat.notify(1, mBuilder.build());
            }
        }
        //audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

        if(remoteMessage.getNotification() != null) {
            Log.d(TAG, "firestore: Message notification body: " + remoteMessage.getNotification().getBody());
        }
    }

    public void onCompletion(MediaPlayer mp) {

    }

    /*
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }
    */
}