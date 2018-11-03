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
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * Settings.
 *
 * @author fredrik.laapotti@gmail.com
 * @version 0.1.181103
 * @since 0.1.181103
 */
public class SettingsFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private static final String TAG = SettingsFragment.class.getSimpleName();

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private EditText txt_email;
    private EditText txt_password;

    private Button btn_userCreate;
    private Button btn_userLogin;
    private Button btn_userLogout;
    private Button btn_userVerify;

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
        /*
        if (getArguments() != null) {
            String mParam1 = getArguments().getString(ARG_PARAM1);
            String mParam2 = getArguments().getString(ARG_PARAM2);
        }
        */
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        txt_email = v.findViewById(R.id.txt_email);
        txt_password = v.findViewById(R.id.txt_password);
        TextView txt_token = v.findViewById(R.id.txt_token);
        Button btn_setEmail = v.findViewById(R.id.btn_setEmail);
        Button btn_fetchToken = v.findViewById(R.id.btn_fetchToken);

        btn_userCreate = v.findViewById(R.id.btn_userCreate);
        btn_userLogin = v.findViewById(R.id.btn_userLogin);
        btn_userLogout = v.findViewById(R.id.btn_userLogout);
        btn_userVerify = v.findViewById(R.id.btn_userVerify);

        updateUI(currentUser);

        sharedPreferences = Objects.requireNonNull(getActivity()).getPreferences(Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        String token = sharedPreferences.getString("token","-1");
        Log.d(TAG,"Shared preferences token: " + token);
        txt_token.setText(token);

        String userEmail = sharedPreferences.getString("email","");
        if(userEmail.equals("")) {
            txt_email.setText("no email registered");
        } else {
            txt_email.setText(userEmail);
        }

        btn_userCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: string checking
                mAuth.createUserWithEmailAndPassword(txt_email.getText().toString(), txt_password.getText().toString())
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(task.isSuccessful()) {
                                    Log.i(TAG, "auth: createuserwithemail: success");
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    updateUI(user);
                                } if(task.getException() instanceof FirebaseAuthUserCollisionException) {
                                    Log.d(TAG, "auth: createuserwithemail: user already exists");
                                } else {
                                    Log.d(TAG, "auth: createuserwithemail: failed with " + Objects.requireNonNull(task.getException()).getMessage());
                                    updateUI(null);
                                }
                            }
                        });
            }
        });

        btn_userLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: error check fields
                mAuth.signInWithEmailAndPassword(txt_email.getText().toString(), txt_password.getText().toString())
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(task.isSuccessful()) {
                                    Log.i(TAG, "auth: signinwithemail: success");
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    updateUI(user);
                                } else {
                                    Log.d(TAG, "auth: signinwithemail: failed");
                                    updateUI(null);
                                }

                                if(!task.isSuccessful()) {
                                    Log.d(TAG, "auth: signinwithemail: failed due to task not successful");
                                }
                            }
                        });
            }
        });

        btn_userLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "auth: signout");
                mAuth.signOut();
                updateUI(null);
            }
        });

        btn_userVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentUser != null) {
                    if (!currentUser.isEmailVerified()) {
                        Log.i(TAG, "auth: current user not email verified, calling sendEmailVerification()");
                        currentUser.sendEmailVerification();
                    } else {
                        Log.i(TAG, "auth: current user email verified, exiting function");
                    }
                }
            }
        });

        // TODO: implement other methods to deprecate this one
        btn_setEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                for(Zone zone : Constants.zoneArrayList) {
                    zone.setSubscribed(false);
                }
                editor.putString("email", txt_email.getText().toString()).apply();
                //Log.i(TAG, "firestore geofence: geofencesJSON in SharedPrefs = " + sharedPreferences.getString("geofencesJSON", ""));

                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("email", txt_email.getText().toString());
                userInfo.put("lastUpdated", Timestamp.now());

                db.collection("users").document(txt_email.getText().toString())
                        .set(userInfo, SetOptions.merge())
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "Snapshot written");
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d("MINDEBUG", "Error adding document");
                            }
                });
                CommonFunctions.updateSubscriptionsFromFirestore("users", sharedPreferences.getString("email", ""));
            }
        });

        btn_fetchToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if(task.isSuccessful()) {
                            String token = Objects.requireNonNull(task.getResult()).getToken();
                            Log.i(TAG, "firestore: successfully fetched new token: " + token);
                            editor.putString("token", token).apply();
                            txt_token.setText(token);
                        }
                    }
                });
            }
        });

        return v;
    }

    private void updateUI(FirebaseUser firebaseUser) {
        if(firebaseUser != null) {
            btn_userCreate.setVisibility(View.GONE);
            btn_userLogin.setVisibility(View.GONE);
            btn_userLogout.setVisibility(View.VISIBLE);

            if(firebaseUser.isEmailVerified()) {
                btn_userVerify.setVisibility(View.GONE);
            } else {
                btn_userVerify.setVisibility(View.VISIBLE);
            }

            Log.i(TAG, "auth: firebaseUser != null");
            Log.i(TAG, "auth: firebaseuser: " + firebaseUser.getEmail() + " " + firebaseUser.getMetadata());
        } else {
            btn_userCreate.setVisibility(View.VISIBLE);
            btn_userLogin.setVisibility(View.VISIBLE);
            btn_userLogout.setVisibility(View.GONE);
            btn_userVerify.setVisibility(View.GONE);

            Log.i(TAG, "auth: firebaseUser == null");
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
}