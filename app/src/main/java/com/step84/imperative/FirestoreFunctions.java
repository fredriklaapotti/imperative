package com.step84.imperative;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;
import java.util.Objects;

public class FirestoreFunctions {
    private static final String TAG = "FirestoreFunctions";

    public interface FirestoreListener {
        public void onStart();

        public void onSuccess();

        public void onFailed();
    }

    public static void updateZones(FirebaseUser firebaseUser, final FirestoreListener listener) {
        listener.onStart();
        Log.i(TAG, "owl: interface: 1");

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(Constants.DATABASE_COLLECTION_ZONES).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()) {
                            Constants.zoneArrayList.clear();
                            Log.i(TAG, "owl: interface: 2");
                            for(QueryDocumentSnapshot document : task.getResult()) {
                                String id = Objects.requireNonNull(document.getId());
                                String name = Objects.requireNonNull(document.get("name")).toString();
                                double lat = Objects.requireNonNull(document.getGeoPoint("latlng")).getLatitude();
                                double lng = Objects.requireNonNull(document.getGeoPoint("latlng")).getLongitude();
                                double radius = Objects.requireNonNull(document.getDouble("radius"));
                                boolean subscribed = false;

                                Constants.zoneArrayList.add(new Zone(id, name, new LatLng(lat, lng), radius, subscribed));

                                Log.i(TAG, "owl: interface: document = " + document.get("name"));
                            }

                            Log.i(TAG, "owl: interface: Constants.zoneArrayList = " + Constants.zoneArrayList.toString());
                            if(firebaseUser != null) {
                                db.collection(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS)
                                        .whereEqualTo(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS_USER, firebaseUser.getUid())
                                        .whereEqualTo(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS_ACTIVE, true)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if(task.isSuccessful()) {
                                                    List<DocumentSnapshot> snapshotList = task.getResult().getDocuments();
                                                    for(DocumentSnapshot snapshot : snapshotList) {
                                                        Log.i(TAG, "owl: interface: found subscription for user = " + firebaseUser.getUid() + " for zone = " + snapshot.get("zone"));
                                                        for(Zone zone: Constants.zoneArrayList) {
                                                            if(zone.getId().equals(snapshot.get(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS_ZONE))) {
                                                                zone.setSubscribed(true);
                                                                Subscription subscription = snapshot.toObject(Subscription.class);
                                                                zone.setSettings(subscription.settings);
                                                                Log.i(TAG, "owl: interface: updated subscriber flag for zone = " + zone.getName());
                                                            }
                                                        }
                                                    }
                                                    listener.onSuccess();
                                                } else {
                                                    Log.d(TAG, "owl: failed to update subscriptions in updateFromFirestore()");
                                                }
                                            }
                                        });
                            }

                            listener.onSuccess();
                        } else {
                            Log.i(TAG, "owl: interface: failed");
                            listener.onFailed();
                        }
                    }
                });
        Log.i(TAG, "owl: interface: 3");
    }

    public static void subscribe(FirebaseUser firebaseUser, Zone zone, final FirestoreListener listener) {
        // Subscribe
    }

    public static void unsubscribe(FirebaseUser firebaseUser, Zone zone, final FirestoreListener listener) {

    }
}
