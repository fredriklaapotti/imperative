package com.step84.imperative;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class AlarmService extends IntentService {

    public AlarmService() {
        super("AlarmService");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        Log.d(AlarmService.ACTIVITY_SERVICE, "i service");
        String dataString = workIntent.getDataString();
    }
}