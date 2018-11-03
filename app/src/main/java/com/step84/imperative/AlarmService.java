/*
 * Service for monitoring alarms.
 * Currently not used since we handle alarms with Firebase Cloud Messaging.
 * Might be needed as background service because of stricter Android API?
 */
package com.step84.imperative;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

/**
 * Class to handle the intents
 *
 * @author fredrik.laapotti@gmail.com
 * @version 0.1.181103
 * @since 0.1.181103
 *
 */
public class AlarmService extends IntentService {

    /**
     * Dummy constructor.
     */
    public AlarmService() {
        super("AlarmService");
    }

    /**
     * Receive intent
     *
     * @param workIntent intent from manifest
     */
    @Override
    protected void onHandleIntent(Intent workIntent) {
        Log.d(AlarmService.ACTIVITY_SERVICE, "i service");
    }
}