package com.step84.imperative;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class CommonFunctions {
    private CommonFunctions() {};

    public static void updateSubscriptionsFromFirestore(String tag, String collection, String document) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference documentReference = db.collection(collection).document(document);
        documentReference.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot documentSnapshot = task.getResult();
                            if (documentSnapshot.exists()) {
                                for(Map.Entry<String, Object> zone : documentSnapshot.getData().entrySet()) {
                                    String key = zone.getKey();
                                    Log.i(tag, "firestore geofence: update subscriptions for user: " + zone.getValue());
                                    if(key.contains("topics")) {
                                        @SuppressWarnings("unchecked")
                                        Map<Object, Object> topics = (Map<Object, Object>) zone.getValue();
                                        for(Map.Entry<Object, Object> oneTopic : topics.entrySet()) {
                                            Log.i(tag, "firestore geofence: looping topics from database: key = " + oneTopic.getKey() + ", value = " + oneTopic.getValue());

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
                                                    Log.i(tag, "firestore geofence: subscribed to " + zoneList.getName());
                                                    //Log.i(tag, "firestore geofence: found match for " + oneTopic.getKey());
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
                                Log.i(tag,"firestore geofence: failed to fetch document for subscribing to topics");
                            }
                        }
                    }
                });
        /**
         * END CODE BLOCK
         * CODE BLOCK: GET SUBSCRIBED TOPICS FROM DATABASE AND ADD TO CONSTANTS
         */

    }

    public static String userPermissions(FirebaseUser firebaseUser) {
        if(firebaseUser != null && firebaseUser.isEmailVerified()) {
            return "verified";
        }
        return "unknown";
    }
}
