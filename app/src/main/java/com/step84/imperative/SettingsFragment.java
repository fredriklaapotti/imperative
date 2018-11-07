package com.step84.imperative;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.w3c.dom.Document;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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

    private String documentId;

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
        //TextView txt_token = v.findViewById(R.id.txt_token);
        //Button btn_setEmail = v.findViewById(R.id.btn_setEmail);
        //Button btn_fetchToken = v.findViewById(R.id.btn_fetchToken);

        btn_userCreate = v.findViewById(R.id.btn_userCreate);
        btn_userLogin = v.findViewById(R.id.btn_userLogin);
        btn_userLogout = v.findViewById(R.id.btn_userLogout);
        btn_userVerify = v.findViewById(R.id.btn_userVerify);

        currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);

        sharedPreferences = Objects.requireNonNull(getActivity()).getPreferences(Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        String token = sharedPreferences.getString("token","-1");
        Log.d(TAG,"Shared preferences token: " + token);
        //txt_token.setText(token);

        btn_userCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!validateForm()) {
                    return;
                }

                mAuth.createUserWithEmailAndPassword(txt_email.getText().toString(), txt_password.getText().toString())
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(task.isSuccessful()) {
                                    Log.i(TAG, "auth: createuserwithemail: success");
                                    AuthCredential credential = EmailAuthProvider.getCredential(txt_email.getText().toString(), txt_password.getText().toString());
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    mAuth.getCurrentUser().linkWithCredential(credential)
                                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                                @Override
                                                public void onComplete(@NonNull Task<AuthResult> task) {
                                                    if(task.isSuccessful()) {
                                                        Log.i(TAG, "owl-user: linked anonymous account with " + txt_email.getText().toString());
                                                    } else {
                                                        Log.d(TAG, "owl-user: failed to link anonymous account with " + txt_email.getText().toString());
                                                    }
                                                }
                                            });
                                    updateUI(user);

                                    Map<String, Object> newUser = new HashMap<>();
                                    newUser.put(Constants.DATABASE_COLLECTION_USERS_CREATED, Timestamp.now());
                                    newUser.put(Constants.DATABASE_COLLECTION_USERS_FIELD_LASTUPDATED, Timestamp.now());
                                    newUser.put(Constants.DATABASE_COLLECTION_USERS_FIELD_EMAIL, txt_email.getText().toString());
                                    db.collection(Constants.DATABASE_COLLECTION_USERS)
                                            .add(newUser)
                                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                @Override
                                                public void onSuccess(DocumentReference documentReference) {
                                                    Log.i(TAG, "owl-user: added new user to Firestore");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.i(TAG, "owl-user: failed to add user to Firestore");
                                                }
                                            });

                                    CommonFunctions.updateSubscriptionsFromFirestore("users", txt_email.getText().toString());
                                } if(task.getException() instanceof FirebaseAuthUserCollisionException) {
                                    Log.d(TAG, "auth: createuserwithemail: user already exists");
                                } else {
                                    Toast.makeText(getContext(), R.string.error_createUserFailed, 5).show();
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
                if(!validateForm()) {
                    return;
                }

                mAuth.signInWithEmailAndPassword(txt_email.getText().toString(), txt_password.getText().toString())
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(task.isSuccessful()) {
                                    Log.i(TAG, "auth: signinwithemail: success");
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    AuthCredential credential = EmailAuthProvider.getCredential(txt_email.getText().toString(), txt_password.getText().toString());
                                    mAuth.getCurrentUser().linkWithCredential(credential)
                                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                                @Override
                                                public void onComplete(@NonNull Task<AuthResult> task) {
                                                    if(task.isSuccessful()) {
                                                        Log.i(TAG, "owl-user: linked anonymous account with " + txt_email.getText().toString());
                                                    } else {
                                                        Log.d(TAG, "owl-user: failed to link anonymous account with " + txt_email.getText().toString());
                                                    }
                                                }
                                            });
                                    txt_password.setText("");
                                    updateUI(user);
                                    updateUserDb("login");
                                    /*
                                    CollectionReference usersRef = db.collection(Constants.DATABASE_COLLECTION_USERS);
                                    Query userDocument = usersRef.whereEqualTo(Constants.DATABASE_COLLECTION_USERS_FIELD_EMAIL, txt_email.getText().toString());
                                    userDocument.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                        @Override
                                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                            List<DocumentSnapshot> snapshotsList = queryDocumentSnapshots.getDocuments();
                                            for(DocumentSnapshot snapshot : snapshotsList) {
                                                Log.i(TAG, "owl-user: testar lista");
                                            }
                                        }
                                    });
                                    */
                                    CommonFunctions.updateSubscriptionsFromFirestore("users",txt_email.getText().toString());
                                } else {
                                    Toast.makeText(getContext(), R.string.error_loginUserFailed, 5).show();
                                    editor.putString(Constants.SP_EMAIL, txt_password.getText().toString()).apply();
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


        Log.i(TAG, "owl-user: current user info: " + currentUser.getUid());

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









        btn_userLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "auth: signout");
                mAuth.signOut();
                sharedPreferences.edit().putString(Constants.SP_EMAIL, txt_email.getText().toString()).apply(); // If user logs out, uses email in text. Add security.
                updateUI(null);
                //updateUserDb();
                for(Zone zone : Constants.zoneArrayList) {
                    zone.setSubscribed(false);
                }
                CommonFunctions.updateSubscriptionsFromFirestore("users", sharedPreferences.getString(Constants.SP_EMAIL, ""));
            }
        });

        btn_userVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharedPreferences.edit().putString(Constants.SP_EMAIL, txt_email.getText().toString()).apply();
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

        updateUserDb("create");

        return v;
    }

    private void updateUserDb(String action) {
        for(Zone zone : Constants.zoneArrayList) {
            zone.setSubscribed(false);
        }
        editor.putString(Constants.SP_EMAIL, txt_email.getText().toString()).apply();

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("email", txt_email.getText().toString());
        userInfo.put("lastUpdated", Timestamp.now());

        // EXPERIMENT 181106
        /*
        if(action.equals("create")) {
            Query userDocument = db.collection(Constants.DATABASE_COLLECTION_USERS).whereEqualTo(Constants.DATABASE_COLLECTION_USERS_FIELD_EMAIL, txt_email.getText().toString());
            Log.i(TAG, "owl-user: userDocument = " + userDocument);
            userDocument.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if(task.isSuccessful()) {
                        for(QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                            Log.i(TAG, "owl-user: documentSnapshot.getData() = " + documentSnapshot.getData().get("email"));
                        }
                    } else {
                        Log.i(TAG, "owl-user: documentSnapshot.getData() failed");
                    }
                }
            });
        }
        */
        // END EXPERIMENT


        db.collection("users").document(txt_email.getText().toString())
                .set(userInfo, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Created or updated user document");
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "Error adding document");
            }
        });
    }

    private void updateUI(FirebaseUser firebaseUser) {
        if(CommonFunctions.userPermissions(firebaseUser).equals("verified") || CommonFunctions.userPermissions(firebaseUser).equals("registered")) {
            btn_userCreate.setVisibility(View.GONE);
            btn_userLogin.setVisibility(View.GONE);
            btn_userLogout.setVisibility(View.VISIBLE);
            txt_password.setVisibility(View.GONE);

            if(firebaseUser.isEmailVerified()) {
                btn_userVerify.setVisibility(View.GONE);
            } else {
                btn_userVerify.setVisibility(View.VISIBLE);
            }

            editor = getActivity().getPreferences(Context.MODE_PRIVATE).edit();
            editor.putString(Constants.SP_EMAIL, firebaseUser.getEmail()).apply();

            Log.i(TAG, "auth: firebaseUser != null");
            Log.i(TAG, "auth: firebaseuser: " + firebaseUser.getEmail() + " " + firebaseUser.getMetadata());
        } else if(CommonFunctions.userPermissions(firebaseUser).equals("anonymous")) {
            btn_userCreate.setVisibility(View.VISIBLE);
            btn_userLogin.setVisibility(View.VISIBLE);
            btn_userLogout.setVisibility(View.GONE);
            btn_userVerify.setVisibility(View.GONE);
            txt_password.setVisibility(View.VISIBLE);
        } else {
            btn_userCreate.setVisibility(View.VISIBLE);
            btn_userLogin.setVisibility(View.VISIBLE);
            btn_userLogout.setVisibility(View.GONE);
            btn_userVerify.setVisibility(View.GONE);
            txt_password.setVisibility(View.VISIBLE);

            editor = getActivity().getPreferences(Context.MODE_PRIVATE).edit();
            editor.putString(Constants.SP_EMAIL, txt_email.getText().toString()).apply();

            Log.i(TAG, "auth: firebaseUser == null");
        }

        sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        String userEmail = sharedPreferences.getString(Constants.SP_EMAIL, "");
        if(userEmail.equals("")) {
            txt_email.setText(R.string.et_loggedout);
        } else {
            txt_email.setText(userEmail);
        }
    }

    private boolean validateForm() {
        boolean valid = true;

        String email = txt_email.getText().toString();
        if(TextUtils.isEmpty(email)) {
            txt_email.setError(getResources().getString(R.string.error_requiredEmail));
            valid = false;
        } else {
            txt_email.setError(null);
        }

        String password = txt_password.getText().toString();
        if(TextUtils.isEmpty(password)) {
            txt_password.setError(getResources().getString(R.string.error_requiredPassword));
            valid = false;
        } else {
            txt_password.setError(null);
        }

        return valid;
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