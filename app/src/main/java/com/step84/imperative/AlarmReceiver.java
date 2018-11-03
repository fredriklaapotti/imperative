/*
 * Placeholder for eventual use as receiver for alarms.
 * Currently handled with Firebase Cloud Messaging.
 */
package com.step84.imperative;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Main class for AlarmReceiver
 *
 * @author fredrik.laapotti@gmail.com
 * @version 0.1.181103
 * @since 0.1.181103
 */
public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = AlarmReceiver.class.getSimpleName();

    /**
     * Receives the intent
     *
     * @param context app context
     * @param intent intent registered in manifest
     *
     */
    // TODO: implement proper receive -> intent handler
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive - action: " + intent.getAction());
    }
}