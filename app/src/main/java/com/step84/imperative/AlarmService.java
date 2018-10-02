package com.step84.imperative;

import android.app.IntentService;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

public class AlarmService extends IntentService {

    public AlarmService() {
        super("AlarmService");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        String dataString = workIntent.getDataString();
    }
}
