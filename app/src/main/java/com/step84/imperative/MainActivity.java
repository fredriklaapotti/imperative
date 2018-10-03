package com.step84.imperative;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.EditText;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity
        extends AppCompatActivity
        implements HomeFragment.OnFragmentInteractionListener,
        ZonesFragment.OnFragmentInteractionListener,
        SettingsFragment.OnFragmentInteractionListener,
        DebugFragment.OnFragmentInteractionListener {

    private FusedLocationProviderClient mFusedLocationClient;
    public Location currentLocation;

    FirebaseFirestore db = FirebaseFirestore.getInstance();


    //int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
    //int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
    //int m = mAudioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, maxVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        Fragment fragment = null;
        FragmentTransaction fragmentTransaction;

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    HomeFragment homeFragment = new HomeFragment();
                    switchFragment(homeFragment);
                    return true;
                case R.id.navigation_zones:
                    ZonesFragment zonesFragment = new ZonesFragment();
                    switchFragment(zonesFragment);
                    return true;
                case R.id.navigation_settings:
                    SettingsFragment settingsFragment = new SettingsFragment();
                    switchFragment(settingsFragment);
                    return true;
                case R.id.navigation_debug:
                    DebugFragment debugFragment = new DebugFragment();
                    switchFragment(debugFragment);
                    return true;
            }
            return false;
        }
    };

    private void switchFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.container, fragment);
        fragmentTransaction.commit();
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* --- START INTENT SECTION --- */
        Intent intent = new Intent(this, AlarmService.class);
        startService(intent);
        /* --- END INTENT SECTION --- */

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        HomeFragment homeFragment = new HomeFragment();
        switchFragment(homeFragment);

        /*
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null) {
                    currentLocation = location;
                    EditText et = findViewById(R.id.debugFragmentText);
                    et.setText(currentLocation.toString());
                }
            }
        });
        */

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    public void onFragmentInteraction(Uri uri) {
        return;
    }
}

