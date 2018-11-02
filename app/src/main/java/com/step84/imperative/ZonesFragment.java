package com.step84.imperative;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ZonesFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ZonesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ZonesFragment extends Fragment implements OnMapReadyCallback, AdapterView.OnItemSelectedListener {

    private static String SENDER_ID = "668055463929";
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    public static GoogleMap mMap;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private static final String TAG = "ZonesFragment";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private List<String> zones = new ArrayList<>();

    private Button btn_enableAlarm;
    private Button btn_disableAlarm;
    private Button btn_toggleSubscription;
    private Button btn_updateSubscriptions;
    private Spinner spinner_zones;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public ZonesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ZonesFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ZonesFragment newInstance(String param1, String param2) {
        ZonesFragment fragment = new ZonesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_zones, container, false);

        final Map<String, Boolean> enableAlarm = new HashMap<>();

        sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        btn_enableAlarm = v.findViewById(R.id.btn_enableAlarm);
        btn_disableAlarm = v.findViewById(R.id.btn_disableAlarm);
        btn_toggleSubscription = v.findViewById(R.id.btn_toggleSubscription);
        /**
         * CONTINUE HERE 181102 17.24
         */
        btn_toggleSubscription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selected = sharedPreferences.getString("selectedZone", "");
                if(btn_toggleSubscription.getText().equals("subscribe")) {
                    FirebaseMessaging.getInstance().subscribeToTopic(selected)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()) {
                                        Map<String, Object> topics = new HashMap<>();
                                        Map<String, Object> topic = new HashMap<>();
                                        topic.put(selected, Timestamp.now());
                                        topics.put("topics", topic);
                                        db.collection("users").document(sharedPreferences.getString("email", "")).set(topics, SetOptions.merge());

                                        /**
                                         * Should rethink this. Perhaps getter of the correct object based on name?
                                         */
                                        for(Zone zone : Constants.zoneArrayList) {
                                            if(zone.getName().equals(selected)) {
                                                zone.setSubscribed(true);
                                            }
                                        }

                                        Log.i(TAG, "firestore: successfully subscribed to topic " + sharedPreferences.getString("selectedZone", "") + "and updated database");
                                        btn_toggleSubscription.setText("unsubscribe");
                                    }
                                }
                            });
                } else if(btn_toggleSubscription.getText().equals("unsubscribe")) {
                    // Unsubscribe from topic

                    FirebaseMessaging.getInstance().unsubscribeFromTopic(selected)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()) {
                                        Map<String, Object> topics = new HashMap<>();
                                        Map<String, Object> topic = new HashMap<>();
                                        topic.put(selected, FieldValue.delete());
                                        topics.put("topics", topic);
                                        db.collection("users").document(sharedPreferences.getString("email", "")).set(topics, SetOptions.merge());

                                        for(Zone zone : Constants.zoneArrayList) {
                                            if (zone.getName().equals(selected)) {
                                                zone.setSubscribed(false);
                                            }
                                        }

                                        Log.i(TAG, "firestore: successfully unsubscribed to topic " + sharedPreferences.getString("selectedZone", "") + "and updated database");
                                        btn_toggleSubscription.setText("subscribe");
                                    }
                                }
                            });

                    Log.i(TAG, "firestore geofence: unsubscribe from: " + selected);
                } else {
                    return;
                }
                updateSubscriptionsFromFirestore();
            }
        });


        btn_updateSubscriptions = v.findViewById(R.id.btn_updateSubscriptions);
        btn_updateSubscriptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSubscriptionsFromFirestore();
            }
        });

        // --- START SPINNER LOGIC ---
        spinner_zones = v.findViewById(R.id.spinner_zones);
        spinner_zones.setOnItemSelectedListener(this);

        // --- BEGIN SUB FIRESTORE ---
        // Method to fetch one document, works if a document by the collection : document exists
        // Used for error testing
        /*
        DocumentReference documentReference = db.collection("zones").document("work");
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if(document.exists()) {
                        Log.i(TAG, "firestore: document data: " + document.getData());
                    } else {
                        Log.d(TAG, "firestore: no document data");
                    }
                } else {
                    Log.d(TAG, "firestore: get failed with", task.getException());
                }
            }
        });
        */

        // Moved to MainActivity
        /*
        db.collection("zones").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String name = document.get("name").toString();
                        double lat = document.getGeoPoint("latlng").getLatitude();
                        double lng = document.getGeoPoint("latlng").getLongitude();
                        LatLng location = new LatLng(lat, lng);
                        Constants.ZONES.put(name, location);
                    }
                } else {
                    Log.d(TAG, "firestore: Error getting documents: ", task.getException());
                }
            }
        });
        */
        // --- END SUB FIRESTORE ---
        //List<String> zones = new ArrayList<>(Constants.ZONES.keySet());
        //zones = new ArrayList<>();
        for(Zone zone : Constants.zoneArrayList) {
            if(zone.getName() != "placeholder") {
                zones.add(zone.getName());
            }
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, zones);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        int spinnerValue = sharedPreferences.getInt("userChoiceSpinner", -1);
        Log.i(TAG, "geofence: userChoiceSpinner = " + spinnerValue);
        spinner_zones.setAdapter(dataAdapter);
        if(spinnerValue != -1 && spinnerValue < zones.size()) {
            spinner_zones.setSelection(spinnerValue);
        } else {
            spinner_zones.setSelection(0);
        }
        // --- END SPINNER LOGIC ---

        // This method works just as well as having it in onActivityCreated
        //final SupportMapFragment myMAPF = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        //myMAPF.getMapAsync(this);

        /**
         * Code before we updated to handle alarms from Firestore with FCM
         */
        /*
        btn_enableAlarm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                enableAlarm.put("alarm_active", true);

                db.collection("users").document("zone_01")
                        .set(enableAlarm)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "Document updated: " + enableAlarm);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, "Document failed to update");
                            }
                        });
            }
        });

        btn_disableAlarm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                enableAlarm.put("alarm_active", false);

                db.collection("users").document("zone_01")
                        .set(enableAlarm)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "Document updated: " + enableAlarm);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, "Document failed to update");
                            }
                        });
            }
        });
        */

        /**
         * END
         * Code before we updated to handle alarms from Firestore with FCM
         */

        // --------------------------- START OLD FIRESTORE LOOP -------------



        // Let's try another move, this one always updates to true
        /*
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if(e != null) {
                    tv.setText("listen failed");
                }

                if(documentSnapshot != null && documentSnapshot.exists()) {
                    docRef.set(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Document updated");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "Document failed to update");
                        }
                    });
                } else {
                    tv.setText("data: null");
                }
            }
        });
        */
        // --------------------------- END FIRESTORE LOOP -------------

        // Probably useless at the moment. Implement push by changing database flags in Firestore instead.
        /*
        AtomicInteger msgId = new AtomicInteger();
        FirebaseMessaging fm = FirebaseMessaging.getInstance();
        fm.send(new RemoteMessage.Builder(SENDER_ID + "@gcm.googleapis.com")
                .setMessageId(Integer.toString(msgId.incrementAndGet()))
                .addData("my message", "hello world")
                .addData("my action", "SAY_HELLO")
                .build());
        */
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(getActivity() != null) {
            //SupportMapFragment mapFragment = (SupportMapFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.map);
            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            if(mapFragment != null) {
                mapFragment.getMapAsync(this);
            }
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng home = new LatLng(57.670897, 15.860455);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(home,15));

        sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.putString("isMapReady", "true").apply();
        //Log.i(TAG, "geofence: onMapReady() in ZonesFragment");
    }

    public static void updateMap(Location location) {
        //Log.i(TAG, "geofence: ENTRY: updateMap()");
        if(location != null) {
            Log.i(TAG, "geofence: updateMap() with arguments: " + location.getLatitude() + " " + location.getLongitude());
            mMap.clear();
            // TEST CIRCLES
            /*
            for (Map.Entry<String, LatLng> entry : Constants.ZONES.entrySet()) {
                //String key = entry.getKey(); // For use with marker below
                LatLng latLng = entry.getValue();
                //Log.i(TAG, "geofence: key = " + key + ", latlng = " + latLng.toString());
                Circle circle = mMap.addCircle(new CircleOptions()
                        .center(latLng)
                        .radius(Constants.GEOFENCE_RADIUS_IN_METERS)
                        .strokeColor(Color.RED)
                        .strokeWidth(5)
                        .fillColor(0x220000FF));
                //Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(key));
            }
            */
            // TEST CIRCLES END

            // NEW TEST, COLOR SUBSCRIBED CIRCLES WITH RED
            int fillColor;
            for (Zone zone : Constants.zoneArrayList) {
                if(zone.getSubscribed()) {
                    fillColor = 0x220000FF;
                } else {
                    fillColor = 0x22FF0000;
                }
                //Log.i(TAG, "geofence: looping through zones " + zone.getName() + zone.getRadius());
                Circle circle = mMap.addCircle(new CircleOptions()
                        .center(zone.getLatlng())
                        .radius(zone.getRadius())
                        .strokeColor(Color.BLUE)
                        .strokeWidth(5)
                        .fillColor(fillColor));
            }
            // END NEW TEST, COLOR SUBSCRIBED CIRCLES WITH RED

            // TEST CIRCLES WITH ARRAY OF OBJECTS
            /*
            for (Zone zone : Constants.zoneArrayList) {
                //Log.i(TAG, "geofence: looping through zones " + zone.getName() + zone.getRadius());
                Circle circle = mMap.addCircle(new CircleOptions()
                        .center(zone.getLatlng())
                        .radius(zone.getRadius())
                        .strokeColor(Color.RED)
                        .strokeWidth(5)
                        .fillColor(0x220000FF));
            }
            */
            // END TEST CIRCLES WITH ARRAY OF OBJECTS
            MarkerOptions mp = new MarkerOptions();
            mp.position(new LatLng(location.getLatitude(), location.getLongitude()));
            mMap.addMarker(mp);
            // Code below works but gets tiresome when changing perspective. Use? Yes/No?
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        String userEmail = sharedPreferences.getString("email", "");
        updateSubscriptionsFromFirestore();

        String selected = parent.getItemAtPosition(pos).toString();
        int userChoice = parent.getSelectedItemPosition();
        editor.putString("selectedZone", selected).apply();
        editor.putInt("userChoiceSpinner", userChoice).apply();
        //Log.i(TAG, "firestore geofence: selected value in SharedPreferences = " + sharedPreferences.getString("selectedZone", ""));

        //Log.i(TAG, "firestore geofence: Constants.zoneArrayList = " + Constants.zoneArrayList.toString());

        /**
         * Iterate over all the known zones
         * The zoneArrayList is populated in MainActivity.populateGeofencesFromFirestore
         */
        for(Zone zone : Constants.zoneArrayList) {
            if(zone.getName().equals(selected)) {
                if(zone.getSubscribed()) {
                    btn_toggleSubscription.setText("unsubscribe");
                } else {
                    btn_toggleSubscription.setText("subscribe");
                }
            }
        }

        /**
         * CODE BLOCK: GET SUBSCRIBED TOPICS FROM DATABASE AND ADD TO CONSTANTS
         * Moved to MainActivity
         */
        /*
        DocumentReference documentReference = db.collection("users").document(userEmail);
        documentReference.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot documentSnapshot = task.getResult();
                            if (documentSnapshot.exists()) {
                                //Object zonesList = documentSnapshot.getData().get("topics");
                                for(Map.Entry<String, Object> zone : documentSnapshot.getData().entrySet()) {
                                    String key = zone.getKey();
                                    if(key.contains("topics")) {
                                        Map<Object, Object> topics = (Map<Object, Object>) zone.getValue();
                                        //Log.i(TAG, "firestore geofence: looping through object object = " + zone.getValue());
                                        for(Map.Entry<Object, Object> oneTopic : topics.entrySet()) {
                                            Log.i(TAG, "firestore geofence: looping topics from database: key = " + oneTopic.getKey() + ", value = " + oneTopic.getValue());
                                            //Constants.subscribedTopics.add(oneTopic.getValue().toString());
                                        }
                                    }
                                }
                            } else {
                                Log.i(TAG,"firestore geofence: failed to fetch document");
                            }
                        }
                    }
                });
                */
        /**
         * END CODE BLOCK
         * CODE BLOCK: GET SUBSCRIBED TOPICS FROM DATABASE AND ADD TO CONSTANTS
         */


        // Code below works, but implement subscription in database
        /*
        for(String topic : sharedPreferences.getAll().keySet()) {
            Log.i(TAG, "firestore: unsubscribing from: " + topic);
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic);
        }

        FirebaseMessaging.getInstance().subscribeToTopic(selected)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            editor.putBoolean(sharedPreferences.getString("selectedZone", ""), true).apply();
                            Log.i(TAG, "firestore: successfully subscribed to topic " + sharedPreferences.getString("selectedZone", ""));
                        }
                    }
        });
        */

        updateSubscriptionsFromFirestore();
    }

    public void onNothingSelected(AdapterView<?> parent) {

    }

    /**
     * Instead of having a separate button to do this we should perhaps do it on a subscribe button click?
     */
    private void updateSubscriptionsFromFirestore() {

        /**
         * CODE BLOCK: GET SUBSCRIBED TOPICS FROM DATABASE AND ADD TO CONSTANTS
         */
        DocumentReference documentReference = db.collection("users").document(sharedPreferences.getString("email",""));
        documentReference.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot documentSnapshot = task.getResult();
                            if (documentSnapshot.exists()) {
                                //Object zonesList = documentSnapshot.getData().get("topics");
                                for(Map.Entry<String, Object> zone : documentSnapshot.getData().entrySet()) {
                                    String key = zone.getKey();
                                    if(key.contains("topics")) {
                                        Map<Object, Object> topics = (Map<Object, Object>) zone.getValue();
                                        //Log.i(TAG, "firestore geofence: looping through object object = " + zone.getValue());
                                        for(Map.Entry<Object, Object> oneTopic : topics.entrySet()) {
                                            Log.i(TAG, "firestore geofence: looping topics from database: key = " + oneTopic.getKey() + ", value = " + oneTopic.getValue());
                                            //Constants.subscribedTopics.add(oneTopic.getValue().toString()); // Should be deprecated, use the zone objects instead

                                            /**
                                             * So this became quite messy with nested loops
                                             * However, at the moment it's good enough to iterate over the small set from Firestore
                                             * and do most of the comparisons on the local zoneArrayList
                                             * The other way around would involve calling Firestore for _all_ the local stored zones
                                             */
                                            for(Zone zoneList : Constants.zoneArrayList) {
                                                //Log.i(TAG, "firestore geofence: inner loop should trigger");
                                                if(zoneList.getName().equals(oneTopic.getKey())) {
                                                    zoneList.setSubscribed(true);
                                                    Log.i(TAG, "firestore geofence: found match for " + oneTopic.getKey());
                                                    if(zoneList.getName().equals(spinner_zones.getSelectedItem().toString())) {
                                                        btn_toggleSubscription.setText("unsubscribe");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Log.i(TAG,"firestore geofence: failed to fetch document for subscribing to topics");
                            }
                        }
                    }
                });
        /**
         * END CODE BLOCK
         * CODE BLOCK: GET SUBSCRIBED TOPICS FROM DATABASE AND ADD TO CONSTANTS
         */

    }
}