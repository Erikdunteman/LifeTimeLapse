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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class EditNameDialog extends DialogFragment {

    private static final String TAG = "EditNameDialog";


    private Project mProject;

    //widgets
    private EditText nameEdit;

    //vars
    private Context mContext = getContext();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_editname, container, false);;
        nameEdit = view.findViewById(R.id.NameField);
        mProject = getArguments().getParcelable("Project");
        final String timestamp = mProject.getProjPhotoTag();
        nameEdit.setText(mProject.getProjName());
        Log.d(TAG, "onCreateView: timestamp: " + timestamp);

        TextView editDialog = view.findViewById(R.id.dialogConfirm);
        editDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Renaming Project");
                Toast.makeText(getActivity(), "Project Renamed", Toast.LENGTH_SHORT).show();

                DatabaseReference reference =  FirebaseDatabase.getInstance().getReference().child("projects")
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child(timestamp);
                mProject.setProjName(nameEdit.getText().toString());

                Log.d(TAG, "onClick: new mProject " + mProject.toString());
                reference.setValue(mProject);

                dismiss();

            }
        });

        return view;
    }
}

















