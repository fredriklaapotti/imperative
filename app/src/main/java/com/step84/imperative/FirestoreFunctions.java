package com.step84.imperative;

import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.internal.service.Common;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static com.step84.imperative.ZonesFragment.updateMap;

public class FirestoreFunctions {
    private static final String TAG = "FirestoreFunctions";
    private boolean isSubscribed;

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

    public static void isSubscribed(FirebaseUser currentUser, Zone currentZone, final FirestoreListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        listener.onStart();
        Log.i(TAG, "owl: interface: in isSubscribed()");

        db.collection(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS)
                .whereEqualTo(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS_USER, currentUser.getUid())
                .whereEqualTo(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS_ZONE, currentZone.getId())
                //.whereEqualTo(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS_ACTIVE, true)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()) {
                            List<DocumentSnapshot> snapshotList = task.getResult().getDocuments();
                            Log.i(TAG, "owl: interface: snapshotList = " + snapshotList.size());
                            if(snapshotList.size() > 0) {
                                listener.onSuccess();
                            } else {
                                listener.onFailed();
                            }
                        } else {
                            Log.d(TAG, "owl: interface: failed to check isSubscribed()");
                        }
                    }
                });
    }

    public static void subscribe(FirebaseUser currentUser, Zone currentZone, final FirestoreListener listener) {
        // Subscribe
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        listener.onStart();
        Constants.alreadySubscribed = false;

        db.collection(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS)
                .whereEqualTo(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS_USER, currentUser.getUid())
                .whereEqualTo(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS_ZONE, currentZone.getId())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()) {
                            List<DocumentSnapshot> snapshotList = task.getResult().getDocuments();
                            Log.i(TAG, "owl: interface: snapshotList = " + snapshotList.size());
                            if(snapshotList.size() > 0) {
                                // User is subscribed to zone in database, snapshot is subscribed document, change it to active
                                Constants.alreadySubscribed = true;
                                for(DocumentSnapshot snapshot : snapshotList) {
                                    Log.i(TAG, "owl: interface: tried to subscribe but already subscribed");
                                    db.collection(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS).document(snapshot.getId())
                                            .update("active", true)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isSuccessful()) {
                                                        Log.i(TAG, "owl: interface: subscription exists, updated active field to true and resubscribed");
                                                        FirebaseMessaging.getInstance().subscribeToTopic(currentZone.getName())
                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        if(task.isSuccessful()) {
                                                                            Log.i(TAG, "owl: interface: subscription exists, resubscribed to Firebase Messaging");
                                                                        } else {
                                                                            Log.i(TAG, "owl: interface: subscription exists, failed resubscribing to Firebase Messaging");
                                                                        }
                                                                    }
                                                                });
                                                    } else {
                                                        Log.d(TAG, "owl: interface: subscription existed, failed to update active field");
                                                    }
                                                }
                                            });
                                }
                            } else {
                                // User is not subscribed to zone, continue with subscription
                                Constants.alreadySubscribed = false;

                                FirebaseMessaging.getInstance().subscribeToTopic(currentZone.getName())
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Log.i(TAG, "owl: interface: successfully subscribed to topic");
                                                    Subscription subscription = new Subscription();
                                                    subscription.active = true;
                                                    subscription.user = currentUser.getUid();
                                                    subscription.zone = currentZone.getId();
                                                    subscription.settings = new HashMap<>();
                                                    subscription.settings.put("alarm_override_sound", false);
                                                    subscription.settings.put("alarm_notice", true);

                                                    db.collection(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS)
                                                            .add(subscription)
                                                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                                @Override
                                                                public void onSuccess(DocumentReference documentReference) {
                                                                    Log.i(TAG, "owl: subscription added to database");
                                                                    CommonFunctions.updateFromFirestore(currentUser);
                                                                }
                                                            })
                                                            .addOnFailureListener(new OnFailureListener() {
                                                                @Override
                                                                public void onFailure(@NonNull Exception e) {
                                                                    Log.d(TAG, "owl: failed to add subscription to database");
                                                                }
                                                            });
                                                    listener.onSuccess();
                                                } else {
                                                    Log.d(TAG, "owl: interface: failed subscribeToTopic");
                                                    listener.onFailed();
                                                }
                                            }
                                        });
                            }
                        } else {
                            Log.d(TAG, "owl: interface: failed to check isSubscribed()");
                        }
                    }
                });
    }

    public static void unsubscribe(FirebaseUser currentUser, Zone currentZone, final FirestoreListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        listener.onStart();

        FirebaseMessaging.getInstance().unsubscribeFromTopic(currentZone.getName())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            db.collection(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS)
                                    .whereEqualTo(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS_USER, currentUser.getUid())
                                    .whereEqualTo(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS_ACTIVE, true)
                                    .whereEqualTo(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS_ZONE, currentZone.getId())
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if(task.isSuccessful()) {
                                                List<DocumentSnapshot> snapshotList = task.getResult().getDocuments();
                                                // User
                                                for(DocumentSnapshot snapshot : snapshotList) {
                                                    Log.i(TAG, "owl: snapshot id = " + snapshot.getId());
                                                    db.collection(Constants.DATABASE_COLLECTION_SUBSCRIPTIONS).document(snapshot.getId())
                                                            //.delete()
                                                            .update("active", false)
                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if(task.isSuccessful()) {
                                                                        Log.i(TAG, "owl: successfully deleted subscription document");
                                                                        CommonFunctions.updateFromFirestore(currentUser);
                                                                        listener.onSuccess();
                                                                    } else {
                                                                        Log.d(TAG, "owl: failed to delete subscription document");
                                                                    }
                                                                }
                                                            });
                                                }
                                            } else {
                                                Log.d(TAG, "owl: failed to update subscriptions in updateFromFirestore()");
                                            }
                                        }
                                    });
                        } else {
                            Log.d(TAG, "owl: failed to unsubscribe from topic");
                            listener.onFailed();
                        }
                    }
                });
    }
}
