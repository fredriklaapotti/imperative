/*
 * Collection of app-specific common methods
 */
package com.step84.imperative;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.api.Context;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Project-wide accessible methods.
 * Should not rely on activities or contexts.
 *
 * @author fredrik.laapotti@gmail.com
 * @version 0.1.181103
 * @since 0.1.181103
 */
public class CommonFunctions {
    private static final String TAG = CommonFunctions.class.getSimpleName();

    /**
     * Dummy constructor.
     */
    private CommonFunctions() {}

    /**
     * Updates subscriptions from Firestore database and updates the local variable
     * Constants.zoneArrayList, an ArrayList of Zone
     *
     * @param collection Collection at root level in database.
     * @param document Specific document holding the zones-topics, for now e-mail
     */
    public static void updateSubscriptionsFromFirestore(String collection, String document) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if(!collection.equals("users") || document.equals("")) {
            Log.d(TAG, "firestore geofence: FATAL: trying database call without values");
            return;
        }

        // TODO: make this loop short and tidy
        DocumentReference documentReference = db.collection(collection).document(document);
        documentReference.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot documentSnapshot = task.getResult();
                            if (Objects.requireNonNull(documentSnapshot).exists()) {
                                for(Map.Entry<String, Object> zone : Objects.requireNonNull(documentSnapshot.getData()).entrySet()) {
                                    String key = zone.getKey();
                                    Log.i(TAG, "firestore geofence: update subscriptions for user: " + zone.getValue());
                                    if(key.contains("topics")) {
                                        @SuppressWarnings("unchecked")
                                        Map<Object, Object> topics = (Map<Object, Object>) zone.getValue();
                                        for(Map.Entry<Object, Object> oneTopic : topics.entrySet()) {
                                            Log.i(TAG, "firestore geofence: looping topics from database: key = " + oneTopic.getKey() + ", value = " + oneTopic.getValue());

                                            for(Zone zoneList : Constants.zoneArrayList) {
                                                if(zoneList.getName().equals(oneTopic.getKey())) {
                                                    zoneList.setSubscribed(true);
                                                    Log.i(TAG, "firestore geofence: subscribed to " + zoneList.getName());
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
    }

    /**
     * Update all zones and whether firebaseUser is subscribed to them
     *
     * @param firebaseUser FirebaseUser-object
     */
    public static void updateFromFirestore(FirebaseUser firebaseUser) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //Intent intent = new Intent("firestore-updated");

        // Updates all zones from the database and puts them in zoneArrayList
        db.collection(Constants.DATABASE_COLLECTION_ZONES)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()) {
                            Constants.zoneArrayList.clear();
                            for(QueryDocumentSnapshot document : task.getResult()) {
                                String id = Objects.requireNonNull(document.getId());
                                String name = Objects.requireNonNull(document.get("name")).toString();
                                double lat = Objects.requireNonNull(document.getGeoPoint("latlng")).getLatitude();
                                double lng = Objects.requireNonNull(document.getGeoPoint("latlng")).getLongitude();
                                double radius = Objects.requireNonNull(document.getDouble("radius"));
                                boolean subscribed = false;

                                Constants.zoneArrayList.add(new Zone(id, name, new LatLng(lat, lng), radius, subscribed));
                                Log.i(TAG, "owl-zones: fetched document " + document.get("name") + " with id = " + document.getId() + " and added to Constants.zoneArrayList");
                            }

                            // zoneArrayList updated, check if user is logged in and update subscriptions
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
                                                    Log.i(TAG, "owl: in task.isSuccessful, should only be called once");
                                                    for(DocumentSnapshot snapshot : snapshotList) {
                                                        Log.i(TAG, "owl-user: found subscription for user = " + firebaseUser.getUid() + " for zone = " + snapshot.get("zone"));
                                                        for(Zone zone: Constants.zoneArrayList) {
                                                            if(zone.getId().equals(snapshot.get(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS_ZONE))) {
                                                                zone.setSubscribed(true);
                                                                Subscription subscription = snapshot.toObject(Subscription.class);
                                                                zone.setSettings(subscription.settings);
                                                                Log.i(TAG, "owl-user: updated subscriber flag for zone = " + zone.getName());
                                                            }

                                                        }
                                                    }
                                                } else {
                                                    Log.d(TAG, "owl: failed to update subscriptions in updateFromFirestore()");
                                                }
                                            }
                                        });
                            }
                            //LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                        } else {
                            Log.d(TAG, "owl: firebase user doesn't exist");
                        }
                    }
                });
    }

    /**
     * Update subscriptions based on the separate subscriptions collection
     *
     * @param firebaseUser A FirebaseUser object, safer than passing strings around.
     */
    public static void updateSubscriptionsFromFirestore2(FirebaseUser firebaseUser) {
        Log.i(TAG, "owl: in updateSubscriptionsFromFirestore2()");
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if(firebaseUser == null) {
            Log.d(TAG, "owl: firebase user null in CommonFunctions.updateSubscriptionsFromFirestore2()");
            return;
        }

        db.collection(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS)
                .whereEqualTo(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS_USER, firebaseUser.getUid())
                //.whereEqualTo(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS_ACTIVE, true)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<DocumentSnapshot> snapshotList = queryDocumentSnapshots.getDocuments();
                        for(DocumentSnapshot snapshot : snapshotList) {
                            Log.i(TAG, "owl-user: found subscription for user = " + firebaseUser.getUid() + " for zone = " + snapshot.get("zone"));
                            for(Zone zone: Constants.zoneArrayList) {
                                if(zone.getId().equals(snapshot.get(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS_ZONE))) {
                                    zone.setSubscribed(true);
                                    Log.i(TAG, "owl-user: updated subscriber flag for zone = " + zone.getName());
                                }

                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "owl: failed to fetch subscription documents");
                    }
                });
    }

    public static void updateAllZonesFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //Constants.zoneArrayList.clear();
        db.collection(Constants.DATABASE_COLLECTION_ZONES)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()) {
                            Constants.zoneArrayList.clear();
                            for(QueryDocumentSnapshot document : task.getResult()) {
                                String id = Objects.requireNonNull(document.getId());
                                String name = Objects.requireNonNull(document.get("name")).toString();
                                double lat = Objects.requireNonNull(document.getGeoPoint("latlng")).getLatitude();
                                double lng = Objects.requireNonNull(document.getGeoPoint("latlng")).getLongitude();
                                double radius = Objects.requireNonNull(document.getDouble("radius"));
                                boolean subscribed = false;

                                Constants.zoneArrayList.add(new Zone(id, name, new LatLng(lat, lng), radius, subscribed));
                                Log.i(TAG, "owl-zones: fetched document " + document.get("name") + " with id = " + document.getId() + " and added to Constants.zoneArrayList");
                            }
                        } else {
                            Log.d(TAG, "owl-zones: failed to fetch zone documents");
                        }
                    }
                });
    }

    public static Zone getZoneByName(String name) {
        Zone foundZone = null;

        for(Zone zone : Constants.zoneArrayList) {
            if(zone.getName().equals(name)) {
                foundZone = zone;
            }
        }

        return foundZone;
    }

    /**
     * Check user permissions regardless of app context.
     *
     * @param firebaseUser Needs to be sent from context (fragment).
     * @return For now a list of strings based on authentication.
     */
    public static String userPermissions(FirebaseUser firebaseUser) {
        Log.i(TAG, "owl: in userPermissions()");

        if(firebaseUser != null && firebaseUser.isEmailVerified()) {
            Log.i(TAG, "owl: user = verified");
            return "verified";
        }
        if(firebaseUser != null && !firebaseUser.isEmailVerified() && !firebaseUser.isAnonymous()) {
            Log.i(TAG, "owl: user = registered" + firebaseUser.getEmail());
            return "registered";
        }
        if(firebaseUser != null && firebaseUser.isAnonymous()) {
            Log.i(TAG, "owl: user = anonymous");
            return "anonymous";
        }
        if(firebaseUser == null) {
            Log.i(TAG, "owl: user = guest");
            return "guest";
        }
        return "unknown";
    }


}

                                    /*
                                    db.collection(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS)
                                            .whereEqualTo("user", currentUser.getUid())
                                            .whereEqualTo("zone", activeZone.getId())
                                            .get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                    if(task.isSuccessful()) {
                                                        Log.i(TAG, "owl: set zone. Id = " + activeZone.getId() + ", name = " + activeZone.getName());
                                                    } else {
                                                        Log.d(TAG, "Failed to fetch documents");
                                                    }
                                                }
                                            });
                                    */


// Disable automatic anonymous login, make it manual
        /* else {
            mAuth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Log.i(TAG, "owl-user: successful anonymous login");
                        currentUser = mAuth.getCurrentUser();
                        updateUI(currentUser);

                        Map<String, Object> newUser = new HashMap<>();
                        newUser.put(Constants.DATABASE_COLLECTION_USERS_CREATED, Timestamp.now());
                        newUser.put(Constants.DATABASE_COLLECTION_USERS_FIELD_LASTUPDATED, Timestamp.now());
                        newUser.put(Constants.DATABASE_COLLECTION_USERS_FIELD_EMAIL, "anonymous");
                        db.collection(Constants.DATABASE_COLLECTION_USERS).document(currentUser.getUid())
                                .set(newUser)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.i(TAG, "owl-user: successfully added anonymous user with id = " + currentUser.getUid());
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "owl-user: failed to add anonymous user with id = " + currentUser.getUid());
                                    }
                                });


                        Log.i(TAG, "owl-user: isAnonymous() = " + currentUser.isAnonymous());
                    } */

                /*
        //Log.i(TAG, "owl-user: current user info: " + currentUser.getUid());
        //String documentId;
        db.collection(Constants.DATABASE_COLLECTION_USERS).whereEqualTo("email", "testar").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()) {
                            for(QueryDocumentSnapshot document : task.getResult()) {
                                documentId = document.getId();

                                db.collection(Constants.DATABASE_COLLECTION_USERS).document(documentId)
                                        .update(Constants.DATABASE_COLLECTION_USERS_FIELD_LASTUPDATED, Timestamp.now())
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.i(TAG, "owl-user: Firestore: updated document id = " + documentId);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.i(TAG, "owl-user: Firestore failed to update document with id = " + documentId);
                                            }
                                        });
                            }
                        }
                    }
                });
        */