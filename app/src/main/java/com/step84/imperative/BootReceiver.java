/*
 * Assure app starts on system boot.
 */
package com.step84.imperative;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Handle intent from device startup
 *
 * @author fredrik.laapotti@gmail.com
 * @version 0.1.181103
 * @since 0.1.181103
 */
public class BootReceiver extends BroadcastReceiver {
    /**
     * Receives intent
     *
     * @param context app context
     * @param intent  intent from device
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent myIntent = new Intent(context, MainActivity.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(myIntent);
    }
}