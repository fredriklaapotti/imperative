package com.step84.imperative;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Objects;

/**
 * MainActivity.
 *
 * @author fredrik.laapotti@gmail.com
 * @version 0.1.181103
 * @since 0.1.181103
 */
public class MainActivity
        extends AppCompatActivity
        implements HomeFragment.OnFragmentInteractionListener,
        ZonesFragment.OnFragmentInteractionListener,
        SettingsFragment.OnFragmentInteractionListener,
        OnCompleteListener<Void>,
        DebugFragment.OnFragmentInteractionListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private FusedLocationProviderClient mFusedLocationClient;
    private Location currentLocation;
    private boolean mRequestingLocationUpdates;
    private LocationCallback mLocationCallback;

    private GeofencingClient mGeofencingClient;
    private ArrayList<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;
    private enum PendingGeofenceTask {
        ADD, REMOVE, NONE
    }
    //private static final int JOB_ID = 573;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private PendingGeofenceTask mPendingGeofenceTask = PendingGeofenceTask.NONE;

    private SharedPreferences sharedPreferences;
    private String isMapReady;

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

        Intent intent = new Intent(this, AlarmService.class);
        startService(intent);

        createNotificationChannel();

        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("isMapReady", "false");
        editor.apply();

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<android.location.Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null) {
                    currentLocation = location;
                    Log.i(TAG, "geofence: current location: " + currentLocation.toString());
                }
            }
        });

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if(!Constants.REQUESTINGLOCATIONUPDATES) {
                    startLocationUpdates();
                }

                if(locationResult == null) {
                    return;
                }

                for(Location location : locationResult.getLocations()) {
                    sharedPreferences = getPreferences(Context.MODE_PRIVATE);
                    isMapReady = sharedPreferences.getString("isMapReady", "false");
                    currentLocation = location;
                    if(isMapReady.equals("true")) {
                        ZonesFragment.updateMap(location);
                    } else {
                        Log.i(TAG, "geofence: map not ready");
                    }
                }
            }
        };

        mGeofenceList = new ArrayList<>();
        mGeofencePendingIntent = null;

        populateGeofenceList();
        populateGeofenceListFromFirestore();
        mGeofencingClient = LocationServices.getGeofencingClient(this);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        HomeFragment homeFragment = new HomeFragment();
        switchFragment(homeFragment);

        //BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(!checkPermissions()) {
            requestPermissions();
        } else {
            performPendingGeofenceTask();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
            mRequestingLocationUpdates = true;
            Constants.REQUESTINGLOCATIONUPDATES = true;
        }
    }

    private void performPendingGeofenceTask() {
        Log.i(TAG, "geofence: ENTRY: performPendingGeofenceTask");
        if(mPendingGeofenceTask == PendingGeofenceTask.ADD) {
            addGeofences();
        } else if (mPendingGeofenceTask == PendingGeofenceTask.REMOVE) {
            removeGeoFences();
        }
    }

    @SuppressWarnings("MissingPermission")
    private void addGeofences() {
        Log.i(TAG, "geofence: ENTRY: addGeoFences()");
        mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnCompleteListener(this);
    }

    @SuppressWarnings("MissingPermission")
    private void removeGeoFences() {
        mGeofencingClient.removeGeofences(getGeofencePendingIntent()).addOnCompleteListener(this);
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        Log.i(TAG, "geofence: in getGeofencingRequest");
        return builder.build();
    }

    @Override
    public void onComplete(@NonNull Task<Void> task) {
        mPendingGeofenceTask = PendingGeofenceTask.NONE;
        if(task.isSuccessful()) {
            updateGeofencesAdded(!getGeofencesAdded());
            Log.i(TAG, "geofence: task successful in onComplete()");
        } else {
            String errorMessage = GeofenceErrorMessages.getErrorString(this, task.getException());
            Log.w(TAG, errorMessage);
        }
        startLocationUpdates();
        mRequestingLocationUpdates = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            mRequestingLocationUpdates = false;
        }
    }

    private PendingIntent getGeofencePendingIntent() {
        if(mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        // TODO: Research IntentService vs. JobIntentService
        //Intent intent = new Intent(getApplicationContext(), GeofenceTransitionsJobIntentService.class);
        Intent intent = new Intent(getApplicationContext(), GeofenceTransitionsIntentService.class);
        Log.i(TAG, "geofence: after intent");
        //mGeofencePendingIntent = PendingIntent.getBroadcast(this,0, intent, PendingIntent.FLAG_UPDATE_CURRENT); //According to sample file
        mGeofencePendingIntent = PendingIntent.getService(getApplicationContext(),0, intent, PendingIntent.FLAG_UPDATE_CURRENT); //According to docs
        Log.i(TAG, "geofence: mGeofencePendingIntent: " + mGeofencePendingIntent.toString());
        return mGeofencePendingIntent;
    }

    public void onFragmentInteraction(Uri uri) {
    }

    /**
     * Populate from list in Constants.zoneArrayList.
     * Needed at app startup to provide non-empty list with current code.
     */
    private void populateGeofenceList() {
        Log.i(TAG,"firestore geofence: entered populateGeofenceList()");

        for(Zone zone : Constants.zoneArrayList) {
            mGeofenceList.add(new Geofence.Builder()
                    .setRequestId(zone.getName())
                    .setCircularRegion(
                            zone.getLat(),
                            zone.getLng(),
                            (float)zone.getRadius()
                    )
                    .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build());
        }
    }

    /**
     * Populate from Firestore collection zones and add to Constants.zoneArrayList.
     */
    private void populateGeofenceListFromFirestore() {
        db.collection("zones").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    Log.i(TAG, "firestore geofence: in onComplete loop");
                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                        String name = Objects.requireNonNull(document.get("name")).toString();
                        double lat = Objects.requireNonNull(document.getGeoPoint("latlng")).getLatitude();
                        double lng = Objects.requireNonNull(document.getGeoPoint("latlng")).getLongitude();
                        double radius = Objects.requireNonNull(document.getDouble("radius"));

                        mGeofenceList.add(new Geofence.Builder()
                                .setRequestId(name)
                                .setCircularRegion(
                                        lat,
                                        lng,
                                        (float) radius
                                )
                                .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                                .build());
                        Constants.zoneArrayList.add(new Zone(name, new LatLng(lat, lng), radius, false));
                    }
                    addGeofences();
                } else {
                    Log.d(TAG, "firestore geofence: Error getting documents to populate geofences: ", task.getException());
                }
            }
        });
    }

    private boolean getGeofencesAdded() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.GEOFENCES_ADDED_KEY, false);
    }

    private void updateGeofencesAdded(boolean added) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(Constants.GEOFENCES_ADDED_KEY, added).apply();
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if(shouldProvideRationale) {
            Log.i(TAG, "geofence: displaying rationale again");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
        } else {
            Log.i(TAG, "geofence: requesting permissions");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "geofence: onRequestPermissionsResult");
        if(requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if(grantResults.length <= 0) {
                Log.i(TAG, "geofence: user interaction cancelled");
            } else if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "geofence: permission granted");
                performPendingGeofenceTask();
            } else {
                Log.i(TAG, "geofence: permission denied");
                mPendingGeofenceTask = PendingGeofenceTask.NONE;
            }
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Imperative"; // Should change to getString(R.something..)
            String description = "Notification channel for alarms"; // Should change to getString(R.something..)
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("Imperative", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            Objects.requireNonNull(notificationManager).createNotificationChannel(channel);
        }
    }
}

