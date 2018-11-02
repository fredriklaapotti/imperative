package com.step84.imperative;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.HashMap;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SettingsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static String TAG = "SettingsFragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public SettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SettingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SettingsFragment newInstance(String param1, String param2) {
        SettingsFragment fragment = new SettingsFragment();
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
        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        final EditText txt_email = v.findViewById(R.id.txt_email);
        final TextView txt_token = v.findViewById(R.id.txt_token);
        Button btn_setEmail = v.findViewById(R.id.btn_setEmail);
        Button btn_fetchToken = v.findViewById(R.id.btn_fetchToken);

        // Obviously we should fetch this from database or local storage first
        //txt_email.setText("fredrik.laapotti@gmail.com");

        // --------------------------- START SHARED PREFERENCES -------------
        final SharedPreferences sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        String token = sharedPreferences.getString("token","-1");
        Log.d(TAG,"Shared preferences token: " + token);
        txt_token.setText(token);

        String userEmail = sharedPreferences.getString("email","");
        if(userEmail.equals("")) {
            txt_email.setText("no email registered");
        } else {
            txt_email.setText(userEmail);
        }
        // --------------------------- END SHARED PREFERENCES -------------

        /** TODO: fetch e-mail, token and add to firestore
         *
         */
        btn_setEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putString("email", txt_email.getText().toString()).apply();
                Log.i(TAG, "firestore geofence: geofencesJSON in SharedPrefs = " + sharedPreferences.getString("geofencesJSON", ""));

                /*
                FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if(!task.isSuccessful()) {
                            Log.w("Messaging", "getInstanceId failed", task.getException());
                        }

                        String token = task.getResult().getToken();
                        Log.d("FCM message token", token);
                        editor.putString("token", token);
                        editor.commit();

                        Map<String, Object> deviceInfo = new HashMap<>();
                        deviceInfo.put("email", "fredrik.laapotti@gmail.com");
                        deviceInfo.put("FCMtoken", token);
                        deviceInfo.put("updateTimestamp", Timestamp.now());
                        db.collection("devices").add(deviceInfo).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d("MINDEBUG", "Snapshot written with id:" + documentReference.getId());
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d("MINDEBUG", "Error adding document");
                            }
                        });
                    }
                });
                */
            }
        });

        btn_fetchToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if(task.isSuccessful()) {
                            String token = task.getResult().getToken();
                            Log.i(TAG, "firestore: successfully fetched new token: " + token);
                            editor.putString("token", token).apply();
                            txt_token.setText(token);
                        }
                    }
                });
            }
        });

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("email", txt_email.getText().toString());
        Log.d(TAG,"txt_email value: " + txt_email.getText());

        /*
        db.collection("users")
        .add(userInfo)
        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                    Log.d(TAG, "Snapshot written with id:" + documentReference.getId());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("MINDEBUG", "Error adding document");
            }
        });
        */
        return v;
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
}