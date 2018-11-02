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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import java.util.ArrayList;
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

        final SharedPreferences sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        Button btn_enableAlarm = v.findViewById(R.id.btn_enableAlarm);
        Button btn_disableAlarm = v.findViewById(R.id.btn_disableAlarm);

        // --- START SPINNER LOGIC ---
        Spinner spinner_zones = v.findViewById(R.id.spinner_zones);
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
        List<String> zones = new ArrayList<>(Constants.ZONES.keySet());

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

        final SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("isMapReady", "true").apply();
        Log.i(TAG, "geofence: onMapReady() in ZonesFragment");
    }

    public static void updateMap(Location location) {
        //Log.i(TAG, "geofence: ENTRY: updateMap()");
        if(location != null) {
            Log.i(TAG, "geofence: updateMap() with arguments: " + location.getLatitude() + " " + location.getLongitude());
            mMap.clear();
            // TEST CIRCLES
            for(Map.Entry<String, LatLng> entry : Constants.ZONES.entrySet()) {
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
            }
            // TEST CIRCLES END
            MarkerOptions mp = new MarkerOptions();
            mp.position(new LatLng(location.getLatitude(), location.getLongitude()));
            mMap.addMarker(mp);
            // Code below works but gets tiresome when changing perspective. Use? Yes/No?
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        final SharedPreferences sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        String selected = parent.getItemAtPosition(pos).toString();
        editor.putString("selectedZone", selected).apply();
        int userChoice = parent.getSelectedItemPosition();
        editor.putInt("userChoiceSpinner", userChoice).apply();

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

    }

    public void onNothingSelected(AdapterView<?> parent) {

    }
}