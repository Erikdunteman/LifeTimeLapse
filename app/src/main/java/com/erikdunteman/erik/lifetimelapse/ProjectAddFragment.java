package com.erikdunteman.erik.lifetimelapse;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.erikdunteman.erik.lifetimelapse.models.Project;
import com.erikdunteman.erik.lifetimelapse.models.ProjectDB;
import com.erikdunteman.erik.lifetimelapse.models.User;
import com.erikdunteman.erik.lifetimelapse.utils.DatabaseHelper;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Erik on 1/20/2018.
 */

public class ProjectAddFragment extends Fragment {
    private static final String TAG = "ProjectAddFragment";

    //This will evade the nullpointer exception when adding to a new bundle from MainActivity.
    public ProjectAddFragment() {
        super();
        setArguments(new Bundle());
    }

    private InterstitialAd mInterstitialAd;
    private String mProjFreq;
    private String mProjName;
    private String mProjLengthGoal;
    private ArrayList<String> mProjectNames;
    private ArrayList<String> prevSavedProjectNames;
    private Project  mProject;


    private int Regular = 0;
    public int getRegular() {
        return Regular;
    }
    public void setRegular(int regular) {
        Regular = regular;
    }



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_projectadd, container, false);
        final CheckBox checkbox = view.findViewById(R.id.checkBox);
        final EditText projName = view.findViewById(R.id.ProjNameAdd);
        final TextView freqPrompt = view.findViewById(R.id.FreqPrompt);
        final TextView lengthGoalPrompt = view.findViewById(R.id.LengthGoalPrompt);
        final Spinner projFreq = view.findViewById(R.id.ProjFreqAdd);
        final EditText projLengthGoal = view.findViewById(R.id.ProjLengthGoalAdd);
        Button cancel = view.findViewById(R.id.ProjAddCancel);
        Button save = view.findViewById(R.id.ProjAddCommit);
        Log.d(TAG, "onCreateView: Project Add Fragment Started");

        MobileAds.initialize(getContext(), "ca-app-pub-7411991967781156~6815414311");
        mInterstitialAd = new InterstitialAd(getContext());
        mInterstitialAd.setAdUnitId("ca-app-pub-7411991967781156/3945353173");
        AdRequest request = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build();
        mInterstitialAd.loadAd(request);
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdOpened() {
                //remove previous fragment from the backstack, therefore navigating back
//                 getActivity().getSupportFragmentManager().popBackStack();
            }
        });


        freqPrompt.setVisibility(View.GONE);
        lengthGoalPrompt.setVisibility(View.GONE);
        projFreq.setVisibility(View.GONE);
        projLengthGoal.setVisibility(View.GONE);
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if (isChecked){
                    freqPrompt.setVisibility(View.VISIBLE);
                    lengthGoalPrompt.setVisibility(View.VISIBLE);
                    projFreq.setVisibility(View.VISIBLE);
                    projLengthGoal.setVisibility(View.VISIBLE);
                    Regular = 1;
                    setRegular(Regular);
                }else{
                    freqPrompt.setVisibility(View.GONE);
                    lengthGoalPrompt.setVisibility(View.GONE);
                    projFreq.setVisibility(View.GONE);
                    projLengthGoal.setVisibility(View.GONE);
                    Regular = 0;
                    setRegular(Regular);
                }
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Log.d(TAG, "onClick: Project Add Committed");
                                        String mProjName = projName.getText().toString();
                                        Long timeStamp = new Date().getTime();
                                        final String timeStampString = timeStamp.toString();
                                        if (getRegular() == 1) {
                                            String mProjFreq = projFreq.getSelectedItem().toString();
                                            String mProjLengthGoal = projLengthGoal.getText().toString();
                                            Project project = new Project(mProjName, mProjFreq, "0", mProjLengthGoal, timeStampString);
                                            mProject = project;
                                            Log.d(TAG, "onClick: New Regular project Created: " + project.toString());
                                            addProject(project, mProjName);
                                        } else {
                                            String mProjFreq = "";
                                            String mProjLengthGoal = "";
                                            Project project = new Project(mProjName, mProjFreq, "0", mProjLengthGoal, timeStampString);
                                            mProject = project;
                                            Log.d(TAG, "onClick: New Irregular project Created: " + project.toString());
                                            addProject(project, mProjName);
                                        }

                                        Toast.makeText(getActivity(), "Project Saved!", Toast.LENGTH_SHORT).show();
                                        //remove previous fragment from the backstack, therefore navigating back
                                        getActivity().getSupportFragmentManager().popBackStack();
                                        if (mInterstitialAd.isLoaded()) {
                                            mInterstitialAd.show();
                                        }

                                    }
                                });



                //Original method for add
//                String uID = FirebaseAuth.getInstance().getCurrentUser().getUid();
//
//                //Attempting to upload to projects
//                Log.d(TAG, "onClick: uploading new project to projects");
//                FirebaseDatabase.getInstance().getReference().
//                        child("projects")
//                        .child(uID)
//                        .child(timeStampString)
//                        .setValue(mProjectDB);
//
//                getUserAccountData(timeStampString);



        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Project Add Cancelled.");

                //remove previous fragment from the backstack, therefore navigating back
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        return view;
    }

    private void getUserAccountData(final String timeStampString) {

        Log.d(TAG, "getUserAccountData: On save: getting user account data");

        //Get the user's project names and adding the new timeStampString to it.
        final String uID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(uID);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Log.d(TAG, "onDataChange: entered");
                Log.d(TAG, "onDataChange: datasnapshot: " + dataSnapshot.toString());

                User user = dataSnapshot.getValue(User.class);
                Log.d(TAG, "onDataChange: found user: "
                        + user.toString());
                mProjectNames = user.getProjectNames();
                if(mProjectNames == null){
                    mProjectNames = new ArrayList<String>();
                }
                Log.d(TAG, "onDataChange: recovered projectlist: " + mProjectNames.toString());
                Log.d(TAG, "onDataChange: adding " + timeStampString + " to projectlist");
                mProjectNames.add(timeStampString);
                Log.d(TAG, "onDataChange: new projectlist: " + mProjectNames.toString());

                //Now to update that arraylist of project names in user node
                Log.d(TAG, "onDataChange: updating the database with new mProjectNames list");
                FirebaseDatabase.getInstance().getReference().child("users")
                        .child(uID)
                        .child("projectNames")
                        .setValue(mProjectNames);

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{


        }catch(ClassCastException e){
            Log.e(TAG, "onAttach: ClassCastException " + e.getMessage() );
        }
    }

    //Check to see if contact is null
    private boolean checkStringIfNull(String string){
        if(string.equals("")){
            return false;
        }else{
            return true;
        }
    }

    //New Method
    private void addProject(Project project, String projName){
        Log.d(TAG, "onClick: attempting to save new project.");
        if(checkStringIfNull(projName)){
            Log.d(TAG, "onClick: saving new project:  " + projName);

            DatabaseHelper databaseHelper = new DatabaseHelper(getActivity());

            if(databaseHelper.addProject(project)){
                Toast.makeText(getActivity(), "Project Saved",Toast.LENGTH_LONG).show();
                getActivity().getSupportFragmentManager().popBackStack();
            }else{
                Toast.makeText(getActivity(), "Error Saving",Toast.LENGTH_LONG).show();

            }

        }else{
            Log.d(TAG, "onClick: No contact name to save");
            Toast.makeText(getActivity(), "Give Contact a name, please" ,Toast.LENGTH_LONG).show();

        }
    }


}

