/*
 * Collection of app-specific common methods
 */
package com.step84.imperative;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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

        // TODO: clean up code and find a way to make this loop short and tidy
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

                                            /*
                                             * So this became quite messy with nested loops
                                             * However, at the moment it's good enough to iterate over the small set from Firestore
                                             * and do most of the comparisons on the local zoneArrayList
                                             * The other way around would involve calling Firestore for _all_ the local stored zones
                                             */
                                            for(Zone zoneList : Constants.zoneArrayList) {
                                                //Log.i(TAG, "firestore geofence: inner loop should trigger");
                                                if(zoneList.getName().equals(oneTopic.getKey())) {
                                                    zoneList.setSubscribed(true);
                                                    Log.i(TAG, "firestore geofence: subscribed to " + zoneList.getName());
                                                    //Log.i(TAG, "firestore geofence: found match for " + oneTopic.getKey());
                                                    // This can probably be moved to the fragment? Just do the above for loop again, without subscribing
                                                    // i.e loop local zones, if it's subscribed, change text
                                                    /*
                                                    if(zoneList.getName().equals(selectedItem) {
                                                        btn_toggleSubscription.setText("unsubscribe");
                                                    }
                                                    */
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
     * Check user permissions regardless of app context.
     *
     * @param firebaseUser Needs to be sent from context (fragment).
     * @return For now a list of strings based on authentication.
     */
    public static String userPermissions(FirebaseUser firebaseUser) {
        if(firebaseUser != null && firebaseUser.isEmailVerified()) {
            return "verified";
        }
        return "unknown";
    }
}
