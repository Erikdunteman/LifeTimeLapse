package com.erikdunteman.erik.lifetimelapse;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.erikdunteman.erik.lifetimelapse.models.Project;
import com.erikdunteman.erik.lifetimelapse.models.User;
import com.erikdunteman.erik.lifetimelapse.utils.Delay;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;


public class DeleteDialog extends DialogFragment {

    private static final String TAG = "DeleteDialog";


    private Project mProject;

    private ArrayList<String> mProjectNames;


    //widgets
    private EditText mEmail;

    //vars
    private Context mContext = getContext();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_deleteproject, container, false);;

        mProject = getArguments().getParcelable("Project");
        final String timestamp = mProject.getProjPhotoTag();
        Log.d(TAG, "onCreateView: timestamp: " + timestamp);

        TextView deleteDialog = view.findViewById(R.id.dialogConfirm);
        deleteDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Deleting Project");
                Toast.makeText(getActivity(), "Deleting Project", Toast.LENGTH_SHORT).show();

                //Now to remove photos from the storage
                deleteStorage(timestamp);

            }
        });

        return view;
    }

    private void deleteStorage(final String timestamp) {
        //Get all photoTags in that project
        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("projects")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(timestamp).child("photoNames");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener(){
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: entered");

                ArrayList<String> photoTags = new ArrayList<String>();
                for (DataSnapshot child: dataSnapshot.getChildren()) {
                    photoTags.add(child.getValue().toString());
                }
                Log.d(TAG, "onDataChange: photoTag values" + photoTags.toString());
                if (photoTags != null) {
                    Log.d(TAG, "onDataChange: old photoTags: " + photoTags.toString());

                    //Delete the storage contents based on these tags
                    for (String i : photoTags) {
                        final StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                                .child("users")
                                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                .child(timestamp).child(i);
                        storageReference.delete();
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        Delay.delay(2000, new Delay.DelayCallback() {
            @Override
            public void afterDelay() {
                deleteDatabaseInfo(timestamp);
            }
        });

    }

    private void deleteDatabaseInfo(final String timestamp) {
        //Remove project from projectNames of User database
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        reference.child("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        Log.d(TAG, "onDataChange: entered");
                        Log.d(TAG, "onDataChange: datasnapshot: " + dataSnapshot.toString());

                        User user = dataSnapshot.getValue(User.class);
                        Log.d(TAG, "onDataChange: found user: "
                                + user.toString());
                        mProjectNames = user.getProjectNames();
                        if (mProjectNames == null) {
                            mProjectNames = new ArrayList<String>();
                        }
                        Log.d(TAG, "onDataChange: recovered projectlist: " + mProjectNames.toString());
                        Log.d(TAG, "onDataChange: removing " + timestamp + " to projectlist");
                        mProjectNames.remove(timestamp);
                        for (int i = 0; i < mProjectNames.size(); i++) {
                            if (mProjectNames.get(i) == null) {
                                mProjectNames.remove(i);
                            }
                        }

                        Log.d(TAG, "onDataChange: new projectlist: " + mProjectNames.toString());

                        //Now to update that arraylist of project names in user node
                        Log.d(TAG, "onDataChange: updating the database with new mProjectNames list");
                        FirebaseDatabase.getInstance().getReference().child("users")
                                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                .child("projectNames")
                                .setValue(mProjectNames);

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });

        //Now to delete that project in the projects node
        Log.d(TAG, "onClick: updating the database with new mProjectNames list");
        FirebaseDatabase.getInstance().getReference().child("projects")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(timestamp)
                .removeValue();


        ProjectsMenuFragment fragment = new ProjectsMenuFragment();
        android.support.v4.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
        // replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();

        dismiss();
    }


}

















