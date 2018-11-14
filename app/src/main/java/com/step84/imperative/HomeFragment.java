/*
 * Main fragment.
 */
package com.step84.imperative;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.internal.service.Common;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Main fragment.
 *
 * @author fredrik.laapotti@gmail.com
 * @version 0.1.181103
 * @since 0.1.181103
 */
public class HomeFragment extends Fragment implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private final static String TAG = HomeFragment.class.getSimpleName();
    private MediaRecorder myAudioRecorder;
    private boolean recording = false;
    private String outputFile;
    private Uri downloadUri;

    private Button btn_larmRecord;
    private TextView txt_loggedInAs;

    private SharedPreferences sharedPreferences;
    private StorageReference storageReference;
    private Uri file;
    private UploadTask uploadTask;

    private OnFragmentInteractionListener mListener;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Use to setup authentication.
     *
     * @param savedInstanceState Android default.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
        if (getArguments() != null) {
            String mParam1 = getArguments().getString(ARG_PARAM1);
            String mParam2 = getArguments().getString(ARG_PARAM2);
        }
        */

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        mAuth = FirebaseAuth.getInstance();
        Log.i(TAG, "owl-ui: onCreate()");
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        CommonFunctions.updateSubscriptionsFromFirestore("users", sharedPreferences.getString(Constants.SP_EMAIL, ""));
        CommonFunctions.updateSubscriptionsFromFirestore2(currentUser);
    }

    /**
     * Handle all view-related work.
     *
     * @param inflater Android default.
     * @param container Android default.
     * @param savedInstanceState Android default.
     * @return Android default.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        Log.i(TAG, "owl-ui: onCreateView()");
        TextView txtSelectedZone = v.findViewById(R.id.txt_selectedZone);
        Button btn_larmPreset = v.findViewById(R.id.btn_larmPreset);
        Button btn_larmCustom = v.findViewById(R.id.btn_larmCustom);
        btn_larmRecord = v.findViewById(R.id.btn_larmRecord);
        txt_loggedInAs = v.findViewById(R.id.txt_loggedInAs);
        sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        //Toast.makeText(getContext(), "In onResume", 10);

        updateUI(currentUser);

        String selectedZone = sharedPreferences.getString(Constants.SP_SELECTEDZONE,"");

        for(Zone zone : Constants.zoneArrayList) {
            if(selectedZone.equals(zone.getName()) && zone.getSubscribed()) {
                String zoneText = selectedZone + getString(R.string.spinner_subscribed);
                txtSelectedZone.setText(zoneText);
                break;
            } else {
                String zoneText = selectedZone + getString(R.string.spinner_unsubscribed);
                txtSelectedZone.setText(zoneText);
            }
        }

        btn_larmPreset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "firestore: alarm button clicked");

                if(sharedPreferences.getString(Constants.SP_SELECTEDZONE, "").equals("")) {
                    Log.d(TAG, "firestore: no selected zone");
                    return;
                }
                Map<String, Object> data = new HashMap<>();
                data.put("activated", Timestamp.now());
                data.put("type", "preset");
                data.put("source", sharedPreferences.getString("email",""));
                data.put("zone", sharedPreferences.getString(Constants.SP_SELECTEDZONE, ""));
                data.put("alarmfile", "");

                db.collection("alarms")
                        .add(data)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.i(TAG, "firestore: in addOnSuccessListener()");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.i(TAG, "firestore: in addOnFailureListener()");
                            }
                        });
            }
        });

        btn_larmCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "firestore: alarm larmCustom button clicked");

                Map<String, Object> data = new HashMap<>();
                data.put("activated", Timestamp.now());
                data.put("type", "preset");
                data.put("source", sharedPreferences.getString("email",""));
                data.put("zone", "stadshuset");
                data.put("alarmfile", "");

                db.collection("alarms")
                        .add(data)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.i(TAG, "firestore: in addOnSuccessListener()");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.i(TAG, "firestore: in addOnFailureListener()");
                            }
                        });
            }
        });

        if(ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, 0);
        }

        if(ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/alarmrecording.3gp";

        storageReference = storage.getReference().child(outputFile);

        btn_larmRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!recording) {
                    Log.i(TAG, "audio: recording is true, starting routine");
                    recording = true;

                    myAudioRecorder = new MediaRecorder();
                    myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
                    myAudioRecorder.setOutputFile(outputFile);
                    try {
                        Log.i(TAG, "audio: in try clause, calling prepare() and start()");
                        myAudioRecorder.prepare();
                        myAudioRecorder.start();
                    } catch (IllegalStateException ise) {
                        Log.d(TAG, "audio: recording caught IllegalStateException");
                        return;
                    } catch (IOException ioe) {
                        Log.d(TAG, "audio recording caught IOException");
                        return;
                    }

                    btn_larmRecord.setText("Press again to stop and send");
                } else {
                    recording = false;
                    myAudioRecorder.stop();
                    myAudioRecorder.reset();

                    file = Uri.fromFile(new File(outputFile));
                    uploadTask = storageReference.putFile(file);

                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Log.i(TAG, "audio: uploaded");
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "audioUpload: failed to upload");
                        }
                    });

                    Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw Objects.requireNonNull(task.getException());
                            }

                            return storageReference.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if(task.isSuccessful()) {
                                downloadUri = task.getResult();
                                Log.i(TAG, "audioUpload: task successful, got " + Objects.requireNonNull(downloadUri).toString());

                                if (sharedPreferences.getString(Constants.SP_SELECTEDZONE, "").equals("")) {
                                    Log.d(TAG, "firestore: no selected zone");
                                    return;
                                }

                                /*
                                try {
                                    String encodedUrl = URLEncoder.encode(downloadUri.toString(), "UTF-8");
                                } catch (UnsupportedEncodingException e) {
                                    throw new AssertionError("UTF-8 not supported");
                                }
                                */

                                Map<String, Object> data = new HashMap<>();
                                data.put("activated", Timestamp.now());
                                data.put("type", "record");
                                data.put("source", sharedPreferences.getString("email", ""));
                                data.put("zone", sharedPreferences.getString(Constants.SP_SELECTEDZONE, ""));
                                //data.put("alarmfile", encodedUrl); // TODO: research differences between encoding the URL or leaving as-is
                                data.put("alarmfile", downloadUri.toString());

                                db.collection("alarms")
                                        .add(data)
                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                            @Override
                                            public void onSuccess(DocumentReference documentReference) {
                                                Log.i(TAG, "firestore: added alarm");
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.i(TAG, "firestore: failed to add alarm");
                                            }
                                        });
                            } else {
                                Log.d(TAG, "audioUpload: failed to get download URL");
                            }
                        }
                    });

                    // TODO: I don't think we really need to instances of MediaPlayer
                    MediaPlayer mediaPlayer2 = new MediaPlayer();
                    try {
                        Log.i(TAG, "audio: playback calling setDataSource with " + outputFile);
                        mediaPlayer2.setDataSource(outputFile);
                        //mediaPlayer2.prepareAsync();
                        mediaPlayer2.prepare();
                        mediaPlayer2.start();
                        //mediaPlayer2.release(); // Where should we call release on the audio objects?
                    } catch (IllegalStateException ise) {
                        Log.d(TAG, "audio: playback caught IllegalStateException");
                        return;
                    } catch (IOException ioe) {
                        Log.d(TAG, "audio: playback caught IOException");
                        return;
                    }

                    btn_larmRecord.setText("Record and larm");
                }
            }
        });

        return v;
    }

    /**
     * Show/hide buttons, update text etc.
     *
     * @param user A Firebase user object.
     */
    private void updateUI(FirebaseUser user) {
        Log.i(TAG, "owl-user: userPermissions(user) = " + CommonFunctions.userPermissions(user));
        if(CommonFunctions.userPermissions(user).equals("verified") || CommonFunctions.userPermissions(user).equals("registered")) {
            btn_larmRecord.setVisibility(View.VISIBLE);
            txt_loggedInAs.setText("User: verified or registered as " + currentUser.getEmail());
        } else if(CommonFunctions.userPermissions(user).equals("anonymous")) {
            btn_larmRecord.setVisibility(View.GONE);
            txt_loggedInAs.setText("User: anonymous");
        }
    }

    /**
     * Callback for MediaPlayer, currently unused.
     *
     * @param mp MediaPlayer object.
     */
    public void onCompletion(MediaPlayer mp) {
        //mp = null;
    }

    /**
     * Callback for MediaPlayer, currently unused.
     *
     * @param mp MediaPlayer object.
     */
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        //mp = null;
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