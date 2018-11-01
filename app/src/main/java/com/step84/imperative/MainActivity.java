package com.step84.imperative;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.JobIntentService;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Map;

import static android.support.v4.app.JobIntentService.enqueueWork;
import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;

public class MainActivity
        extends AppCompatActivity
        implements HomeFragment.OnFragmentInteractionListener,
        ZonesFragment.OnFragmentInteractionListener,
        SettingsFragment.OnFragmentInteractionListener,
        OnCompleteListener<Void>, //OnMapReadyCallback,
        DebugFragment.OnFragmentInteractionListener {

    private static final String TAG = "MainActivity";

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private FusedLocationProviderClient mFusedLocationClient;
    public Location currentLocation;
    private boolean mRequestingLocationUpdates;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;

    /* --- START GEOFENCING SETUP ---*/
    private GeofencingClient mGeofencingClient;
    private ArrayList<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;
    private enum PendingGeofenceTask {
        ADD, REMOVE, NONE
    }
    private static final int JOB_ID = 573;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private PendingGeofenceTask mPendingGeofenceTask = PendingGeofenceTask.NONE;
    /* --- END GEOFENCING SETUP ---*/
    private GoogleMap map;
    private ZonesFragment zFragment;

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

        createNotificationChannel();

        final SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("isMapReady", "false");
        editor.apply();

        // --- START FIRESTORE SETTINGS ---
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);
        // --- END FIRESTORE SETTINGS ---

        // --------------------------- START LOCATION TEST -------------
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

        //if(isMapReady != 0) {
            //SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            //mapFragment.getMapAsync(this);
            //MarkerOptions mp = new MarkerOptions();
            //map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
        //}

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if(locationResult == null) {
                    return;
                }
                for(Location location : locationResult.getLocations()) {
                    SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
                    String isMapReady = sharedPreferences.getString("isMapReady", "false");
                    //zFragment = new ZonesFragment();
                    //Log.i(TAG, "geofence: isMapReady: " + isMapReady);
                    //Log.i(TAG, "geofence: new location");
                    currentLocation = location;
                    if(isMapReady.equals("true")) {
                        //Log.i(TAG, "geofence: map is ready, new location");
                        //zFragment.updateMap(location);
                        ZonesFragment.updateMap(location);
                    } else {
                        Log.i(TAG, "geofence: map not ready");
                    }
                }
            }
        };
        // --------------------------- END LOCATION TEST -------------

        // --------------------------- START UPDATE ZONES TEST -------------
        db.collection("zones").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String name = document.get("name").toString();
                        double lat = document.getGeoPoint("latlng").getLatitude();
                        double lng = document.getGeoPoint("latlng").getLongitude();
                        //double radius = document.getDouble("radius");
                        //Log.i(TAG, "geofence: radius values: " + radius);
                        LatLng location = new LatLng(lat, lng);
                        Constants.ZONES.put(name, location);
                        populateGeofenceList();
                    }
                } else {
                    Log.d(TAG, "firestore: Error getting documents: ", task.getException());
                }
            }
        });
        // --------------------------- END UPDATE ZONES TEST -------------

        /* --- START GEOFENCING SECTION ---*/
        mGeofenceList = new ArrayList<>();
        mGeofencePendingIntent = null;
        populateGeofenceList();
        mGeofencingClient = LocationServices.getGeofencingClient(this);
        Log.i(TAG, "geofence: mGeofencingClient: " + mGeofencingClient.toString());
        addGeofences();
        /* --- END GEOFENCING SECTION ---*/

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        HomeFragment homeFragment = new HomeFragment();
        switchFragment(homeFragment);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
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
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
            mRequestingLocationUpdates = true;
        }
    }

    /*
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        LatLng home = new LatLng(57.670897, 15.860455);
        map.addMarker(new MarkerOptions().position(home).title("Marker"));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(home,15));
        Log.i(TAG, "geofence: onMapReady() in MainActivity");
    }

    public void updateMap(Location location) {
        Log.i(TAG, "geofence: ENTRY: updateMap()");
        if(location != null) {
            Log.i(TAG, "geofence: updateMap() with arguments: " + location.getLatitude() + " " + location.getLongitude());
            map.clear();
            MarkerOptions mp = new MarkerOptions();
            mp.position(new LatLng(location.getLatitude(), location.getLongitude()));
            map.addMarker(mp);
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
        }
    }
    */

    private void performPendingGeofenceTask() {
        Log.i(TAG, "geofence: ENTRY: performPendingGeofenceTask");
        if(mPendingGeofenceTask == PendingGeofenceTask.ADD) {
            addGeofences();
        } else if (mPendingGeofenceTask == PendingGeofenceTask.REMOVE) {
            //removeGeoFences();
        }
    }

    @SuppressWarnings("MissingPermission")
    public void addGeofences() {
        Log.i(TAG, "geofence: ENTRY: addGeoFences()");
        mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnCompleteListener(this);
    }

    @SuppressWarnings("MissingPermission")
    private void removeGeoFences() {
        //mGeofencingClient.removeGeofences(getGeofencePendingIntent()).addOnCompleteListener(this);
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
        //Intent intent = new Intent(getApplicationContext(), GeofenceTransitionsJobIntentService.class);
        Intent intent = new Intent(getApplicationContext(), GeofenceTransitionsIntentService.class);
        Log.i(TAG, "geofence: after intent");
        //mGeofencePendingIntent = PendingIntent.getBroadcast(this,0, intent, PendingIntent.FLAG_UPDATE_CURRENT); //According to sample file
        mGeofencePendingIntent = PendingIntent.getService(getApplicationContext(),0, intent, PendingIntent.FLAG_UPDATE_CURRENT); //According to docs
        Log.i(TAG, "geofence: mGeofencePendingIntent: " + mGeofencePendingIntent.toString());
        return mGeofencePendingIntent;
    }

    public void onFragmentInteraction(Uri uri) {
        return;
    }

    private void populateGeofenceList() {
        Log.i(TAG, "geofence: ENTRY populateGeofenceList()");
        for(Map.Entry<String, LatLng> entry : Constants.ZONES.entrySet()) {
            mGeofenceList.add(new Geofence.Builder()
                    .setRequestId(entry.getKey())
                    .setCircularRegion(
                            entry.getValue().latitude,
                            entry.getValue().longitude,
                            Constants.GEOFENCE_RADIUS_IN_METERS
                    )
                    //.setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                    .setExpirationDuration(NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build());
        }
        Log.i(TAG, "geofence: populated list in populateGeofenceList()");
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
            notificationManager.createNotificationChannel(channel);
        }
    }
}

