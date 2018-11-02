package com.step84.imperative;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HomeFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseStorage storage = FirebaseStorage.getInstance();

    final static String TAG = "HomeFragment";
    private MediaRecorder myAudioRecorder;
    private boolean recording = false;
    private String outputFile;
    private String timestampNow;
    private Uri downloadUri;
    private String encodedUrl;
    private TextView txtSelectedZone;

    private AudioManager audioManager;
    private MediaPlayer mediaPlayer;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

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
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
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
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        txtSelectedZone = v.findViewById(R.id.txt_selectedZone);

        Button btnAGF = v.findViewById(R.id.btn_addGeofences);
        btnAGF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).addGeofences();
            }
        });


        // --------------------------- START AUDIO -------------
        audioManager = (AudioManager)getActivity().getSystemService(Context.AUDIO_SERVICE);
        mediaPlayer = MediaPlayer.create(getActivity(),R.raw.alarmfile);
        //mediaPlayer.setOnPreparedListener(HomeFragment.this.onPrepared());
        //mediaPlayer.prepareAsync();

        final int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        final int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        // --------------------------- END AUDIO -------------

        // --------------------------- START FIRESTORE LOOP -------------
        /*
        final DocumentReference docRef = db.collection("users").document("zone_01");
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if(e != null) {
                    Log.d(TAG, "firestore: onEvent() failed");
                }

                if(documentSnapshot != null && documentSnapshot.exists()) {
                    Map<String, Object> map = documentSnapshot.getData();

                    Log.i(TAG, "documentSnapshot data: " + documentSnapshot.getData());
                    String larma = documentSnapshot.getData().get("alarm_active").toString();
                    if(larma == "true") {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                        Log.i(TAG, "firestore: larma == true");
                        mediaPlayer.setOnCompletionListener(HomeFragment.this); // This method plays alarm twice when enabled from zones fragment

                        mediaPlayer.start();
                    } else {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                    }
                    //mediaPlayer.start();
                } else {
                    Log.d(TAG, "documentSnapshot data == null");
                }
            }
        });
        */
        // --------------------------- END FIRESTORE LOOP -------------

        // --------------------------- START UNIT INFO -------------
        //TelephonyManager telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        //String IMEI = telephonyManager.getImei();
        //Log.d("unit info", "Device ID, IMEI:");
        // --------------------------- END UNIT INFO -------------

        // --------------------------- START SHARED PREFERENCES -------------
        sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        String selectedZone = sharedPreferences.getString("selectedZone","");
        //txtSelectedZone.setText(selectedZone);
        for(Zone zone : Constants.zoneArrayList) {
            if(selectedZone.equals(zone.getName()) && zone.getSubscribed()) {
                txtSelectedZone.setText(selectedZone + " - subscribed");
                break;
            } else {
                txtSelectedZone.setText(selectedZone + " - not subscribed");
            }
        }
        // --------------------------- END SHARED PREFERENCES -------------

        /**
         * TODO: rethink where we fetch and update the token
         * Would probably be better to move token fetch to start of activity and monitor updates in background thread
         */
        // --------------------------- START FCM -------------
        // Move to where we register e-mail, SettingsFragment
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
        // --------------------------- END FCM -------------

        Button btn_larmPreset = v.findViewById(R.id.btn_larmPreset);
        Button btn_larmCustom = v.findViewById(R.id.btn_larmCustom);
        final Button btn_larmRecord = v.findViewById(R.id.btn_larmRecord);

        btn_larmPreset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "firestore: alarm button clicked");

                if(sharedPreferences.getString("selectedZone", "").equals("")) {
                    Log.d(TAG, "firestore: no selected zone");
                    return;
                }
                Map<String, Object> data = new HashMap<>();
                //data.put("activated", Timestamp.now());
                data.put("type", "preset");
                data.put("source", sharedPreferences.getString("email",""));
                data.put("zone", sharedPreferences.getString("selectedZone", ""));
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
                //data.put("activated", Timestamp.now());
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

        timestampNow = Timestamp.now().toString();
        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/alarmrecording.3gp";
        //outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/alarmrecording-" + timestampNow + ".3gp"; // Too long?
        final StorageReference storageReference = storage.getReference().child(outputFile);

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
                        // Something
                        Log.d(TAG, "audio: recording caught IllegalStateException");
                        return;
                    } catch (IOException ioe) {
                        Log.d(TAG, "audio recording caught IOException");
                        // Something
                        return;
                    }

                    btn_larmRecord.setText("Press again to stop and send");
                } else {
                    /** BEGIN AUDIO SECTION
                     *
                     */
                    recording = false;
                    myAudioRecorder.stop();
                    myAudioRecorder.reset();
                    //myAudioRecorder.release();
                    //myAudioRecorder = null;

                    final Uri file = Uri.fromFile(new File(outputFile));
                    UploadTask uploadTask = storageReference.putFile(file);

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
                                throw task.getException();
                            }

                            return storageReference.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if(task.isSuccessful()) {
                                downloadUri = task.getResult();
                                Log.i(TAG, "audioUpload: task successful, got " + downloadUri.toString());
                                //editor.putString("larmRecordURL", downloadUri.toString()).apply(); // Might take a while, so in database call, use variable

                                // START EXPERIMENT WHEN TASK IS SUCCESSFUL

                                if (sharedPreferences.getString("selectedZone", "").equals("")) {
                                    Log.d(TAG, "firestore: no selected zone");
                                    return;
                                }
                                try {
                                    encodedUrl = URLEncoder.encode(downloadUri.toString(), "UTF-8");
                                } catch (UnsupportedEncodingException e) {
                                    throw new AssertionError("UTF-8 not supported");
                                }

                                Map<String, Object> data = new HashMap<>();
                                //Map<String, String> data = new HashMap<>();
                                //data.put("activated", Timestamp.now());
                                data.put("type", "record");
                                data.put("source", sharedPreferences.getString("email", ""));
                                data.put("zone", sharedPreferences.getString("selectedZone", ""));
                                //data.put("alarmfile", encodedUrl);
                                data.put("alarmfile", downloadUri.toString());
                                //Log.i(TAG, "audioUpload: alarmfile in SharedPreferences = " + sharedPreferences.getString("larmRecordURL", ""));

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


                                // END EXPERIMENT WHEN TASK IS SUCCESSFUL
                            } else {
                                // Handle errors
                                Log.d(TAG, "audioUpload: failed to get download URL");
                            }
                        }
                    });

                    /** END AUDIO SECTION
                     *
                     */

                    /** BEGIN DATABASE SECTION
                     *
                     */
                    // EXPERIMENT - MOVE TO WHEN WE HAVE URL - IT WORKED!
                    // SAVE FOR THE FUTURE
                    /*
                    if(downloadUri != null) {
                        if (sharedPreferences.getString("selectedZone", "").equals("")) {
                            Log.d(TAG, "firestore: no selected zone");
                            return;
                        }
                        Map<String, Object> data = new HashMap<>();
                        data.put("activated", Timestamp.now());
                        data.put("type", "record");
                        data.put("source", sharedPreferences.getString("email", ""));
                        data.put("zone", sharedPreferences.getString("selectedZone", ""));
                        data.put("alarmfile", downloadUri.toString());
                        //Log.i(TAG, "audioUpload: alarmfile in SharedPreferences = " + sharedPreferences.getString("larmRecordURL", ""));

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
                    */
                    // END EXPERIMENT - MOVE TO WHEN WE HAVE URL
                    /** END DATABASE SECTION
                     *
                     */

                    MediaPlayer mediaPlayer2 = new MediaPlayer();
                    try {
                        Log.i(TAG, "audio: playback calling setDataSource with " + outputFile);
                        mediaPlayer2.setDataSource(outputFile);
                        //mediaPlayer2.prepareAsync();
                        mediaPlayer2.prepare();
                        mediaPlayer2.start();
                        //mediaPlayer2.release(); // Where should we call release on the audio objects?
                    } catch (IllegalStateException ise) {
                        // Something
                        Log.d(TAG, "audio: playback caught IllegalStateException");
                        return;
                    } catch (IOException ioe) {
                        // Something
                        Log.d(TAG, "audio: playback caught IOException");
                        return;
                    }

                    btn_larmRecord.setText("Record and larm");
                }
            }
        });

        return v;
    }

    public void onCompletion(MediaPlayer mp) {
        mp = null;
    }

    public void onPrepared(MediaPlayer mp) {
        mp.start();
        mp = null;
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