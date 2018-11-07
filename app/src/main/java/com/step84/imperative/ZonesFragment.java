package com.step84.imperative;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.location.Location;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.internal.service.Common;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * Zones fragment.
 */
public class ZonesFragment extends Fragment implements OnMapReadyCallback, AdapterView.OnItemSelectedListener {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static GoogleMap mMap;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private static final String TAG = "ZonesFragment";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private final List<String> zones = new ArrayList<>();

    private Button btn_toggleSubscription;
    private Spinner spinner_zones;

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
        /*
        if (getArguments() != null) {
            String mParam1 = getArguments().getString(ARG_PARAM1);
            String mParam2 = getArguments().getString(ARG_PARAM2);
        }
        */
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        CommonFunctions.updateAllZonesFromFirestore();
        CommonFunctions.updateSubscriptionsFromFirestore2(currentUser);
        for(Zone zone : Constants.zoneArrayList) {
            if(!zone.getName().equals("placeholder")) {
                zones.add(zone.getName());
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_zones, container, false);

        sharedPreferences = Objects.requireNonNull(getActivity()).getPreferences(Context.MODE_PRIVATE);
        CommonFunctions.updateSubscriptionsFromFirestore("users", sharedPreferences.getString(Constants.SP_EMAIL, ""));

        btn_toggleSubscription = v.findViewById(R.id.btn_toggleSubscription);
        btn_toggleSubscription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selected = sharedPreferences.getString(Constants.SP_SELECTEDZONE, "");
                if(btn_toggleSubscription.getText().toString().equals(getString(R.string.btn_toggleSubscriptionSubscribe))) {
                    FirebaseMessaging.getInstance().subscribeToTopic(selected)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()) {
                                        Map<String, Object> topics = new HashMap<>();
                                        Map<String, Object> topic = new HashMap<>();
                                        topic.put(selected, Timestamp.now());
                                        topics.put("topics", topic);
                                        db.collection("users").document(sharedPreferences.getString(Constants.SP_EMAIL, "")).set(topics, SetOptions.merge());

                                        // TODO: redesign, is it possible to loop over a list of objects in the object class?
                                        for(Zone zone : Constants.zoneArrayList) {
                                            if(zone.getName().equals(selected)) {
                                                zone.setSubscribed(true);
                                            }
                                        }

                                        Log.i(TAG, "firestore: successfully subscribed to topic " + sharedPreferences.getString(Constants.SP_SELECTEDZONE, "") + "and updated database");
                                        btn_toggleSubscription.setText(R.string.btn_toggleSubscriptionUnsubscribe);
                                    }
                                }
                            });
                } else if(btn_toggleSubscription.getText().toString().equals(getString(R.string.btn_toggleSubscriptionUnsubscribe))) {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(selected)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()) {
                                        Map<String, Object> topics = new HashMap<>();
                                        Map<String, Object> topic = new HashMap<>();
                                        topic.put(selected, FieldValue.delete());
                                        topics.put("topics", topic);
                                        Log.i(TAG, "firestore geofence: TRYING TO UNSUBSCRIBE" + sharedPreferences.getString(Constants.SP_EMAIL, ""));
                                        db.collection("users").document(sharedPreferences.getString(Constants.SP_EMAIL, "")).set(topics, SetOptions.merge());

                                        for(Zone zone : Constants.zoneArrayList) {
                                            if (zone.getName().equals(selected)) {
                                                zone.setSubscribed(false);
                                            }
                                        }

                                        Log.i(TAG, "firestore: successfully unsubscribed to topic " + sharedPreferences.getString(Constants.SP_SELECTEDZONE, "") + "and updated database");
                                        btn_toggleSubscription.setText(R.string.btn_toggleSubscriptionSubscribe);
                                    }
                                }
                            });
                } else {
                    return;
                }
                CommonFunctions.updateSubscriptionsFromFirestore("users", sharedPreferences.getString(Constants.SP_EMAIL, ""));
                updateSubscriptionText();
            }
        });

        if(CommonFunctions.userPermissions(currentUser).equals("guest")) {
            btn_toggleSubscription.setVisibility(View.GONE);
        }

        spinner_zones = v.findViewById(R.id.spinner_zones);
        spinner_zones.setOnItemSelectedListener(this);


        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(Objects.requireNonNull(getContext()), android.R.layout.simple_spinner_item, zones);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        int spinnerValue = sharedPreferences.getInt(Constants.SP_SPINNERSELECTED, -1);
        Log.i(TAG, "geofence: userChoiceSpinner = " + spinnerValue);
        spinner_zones.setAdapter(dataAdapter);
        if(spinnerValue != -1 && spinnerValue < zones.size()) {
            spinner_zones.setSelection(spinnerValue);
        } else {
            spinner_zones.setSelection(0);
        }

        updateSubscriptionText();

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(getActivity() != null) {
            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            if(mapFragment != null) {
                mapFragment.getMapAsync(this);
            }
        }
    }

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
        void onFragmentInteraction(Uri uri);
    }

    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng home = new LatLng(57.670897, 15.860455);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(home,15));

        sharedPreferences = Objects.requireNonNull(getActivity()).getPreferences(Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.putString("isMapReady", "true").apply();
    }

    public static void updateMap(Location location) {
        if(location != null) {
            Log.i(TAG, "geofence: updateMap() with arguments: " + location.getLatitude() + " " + location.getLongitude());
            mMap.clear();

            int fillColor;
            for (Zone zone : Constants.zoneArrayList) {
                if(zone.getSubscribed()) {
                    fillColor = 0x220000FF;
                } else {
                    fillColor = 0x22FF0000;
                }

                Circle circle = mMap.addCircle(new CircleOptions()
                        .center(zone.getLatlng())
                        .radius(zone.getRadius())
                        .strokeColor(Color.BLUE)
                        .strokeWidth(5)
                        .fillColor(fillColor));
            }

            MarkerOptions mp = new MarkerOptions();
            mp.position(new LatLng(location.getLatitude(), location.getLongitude()));
            mMap.addMarker(mp);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        sharedPreferences = Objects.requireNonNull(getActivity()).getPreferences(Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        String selected = parent.getItemAtPosition(pos).toString();
        int userChoice = parent.getSelectedItemPosition();
        editor.putString(Constants.SP_SELECTEDZONE, selected).apply();
        editor.putInt(Constants.SP_SPINNERSELECTED, userChoice).apply();

        updateSubscriptionText();
    }

    public void onNothingSelected(AdapterView<?> parent) {
    }

    public void updateSubscriptionText() {
        Log.i(TAG, "firestore geofence: CALLING updateSubscriptionText");
        btn_toggleSubscription.setText(R.string.btn_toggleSubscriptionSubscribe);
        for(Zone zone : Constants.zoneArrayList) {
            if(zone.getSubscribed() && zone.getName().equals(spinner_zones.getSelectedItem().toString())) {
                Log.i(TAG, "firestore geofence: IN INNER LOOP");
                btn_toggleSubscription.setText(R.string.btn_toggleSubscriptionUnsubscribe);
            }
        }
    }
}