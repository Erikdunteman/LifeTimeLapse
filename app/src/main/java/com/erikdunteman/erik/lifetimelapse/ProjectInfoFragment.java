package com.erikdunteman.erik.lifetimelapse;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.erikdunteman.erik.lifetimelapse.models.Project;
import com.erikdunteman.erik.lifetimelapse.models.ProjectDB;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Erik on 1/20/2018.
 */

public class ProjectInfoFragment extends Fragment {
    private static final String TAG = "ProjectInfoFragment";

    public interface OnProjectPhotoAddListener{
        void OnProjectPhotoAdd(Project con);
    }
    OnProjectPhotoAddListener mPhotoAddListener;

    ImageLoader imageLoader = ImageLoader.getInstance();

    //This will evade the nullpointer exception when adding to a new bundle from MainActivity.
    public ProjectInfoFragment() {
        super();
        setArguments(new Bundle());
    }


    private android.support.v7.widget.Toolbar toolbar;
    private Project mProject;
    private TextView mProjName;
    private CircleImageView mProjImage1;
    private CircleImageView mProjImage2;
    private TextView mProjFreq;
    private TextView mProjLength;
    private TextView mProjLengthGoal;
    private String mProjPhotoTag;
    private String mGalleryPath;
    int recentIndex;
    int firstIndex;






    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_projectinfo, container, false);
        toolbar = view.findViewById(R.id.projectInfoToolbar);
        mProject = getProjectFromBundle();
        if (mProject != null) {
            Log.d(TAG, "onCreateView: recieved project:" + mProject.getProjName());
            mProjPhotoTag = mProject.getProjPhotoTag();
        }
        mProjName = view.findViewById(R.id.projectInfoName);
        mProjFreq = view.findViewById(R.id.projectInfoFreq);
        mProjLength = view.findViewById(R.id.projectInfoLength);
        mProjLengthGoal = view.findViewById(R.id.projectInfoLengthGoal);
        Log.d(TAG, "onCreateView: Project Info Fragment Started");


        //required for setting up the tool bar
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);


        //initialize the first and recent photos into their locations
        CircleImageView recentPic = view.findViewById(R.id.projectInfoImage2);
        CircleImageView firstPic = view.findViewById(R.id.projectInfoImage1);
        setImageView(recentPic,firstPic, mProjPhotoTag);


        //navigation for the backarrow
        ImageView ivBackArrow = view.findViewById(R.id.ivBackArrow);
        ivBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked back arrow.");

                //remove previous fragment from the backstack, therefore navigating back
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        //navigate to the camera fragment to edit
        ImageView ivImageAdd = view.findViewById(R.id.projectInfoImageAdd);
        ivImageAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked photo add icon");
                //pass the contact to the interface and send to MainActivity
                mPhotoAddListener.OnProjectPhotoAdd(mProject);
            }
        });

        //Export project to video
        ImageView ivExport = view.findViewById(R.id.ivExport);
        ivExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ExportDialog dialog = new ExportDialog();
                Bundle args = new Bundle();
                args.putParcelable("Project", mProject);
                dialog.setArguments(args);
                dialog.show(getFragmentManager(),"export");
            }
        });

        //Export project to video
        ImageView ivEdit = view.findViewById(R.id.ivEdit);
        ivEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditNameDialog dialog = new EditNameDialog();
                Bundle args = new Bundle();
                args.putParcelable("Project", mProject);
                dialog.setArguments(args);
                dialog.show(getFragmentManager(),"edit");
            }
        });



        return view;
    }

    private void redefineProjectFromDatabase(String mProjPhotoTag) {
        //Because this info is received as bundle from projects menu, we need to have it update
        //to reflect the database, using the passed phototag in the bundle

        final String uID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("projects")
                .child(uID)
                .child(mProjPhotoTag);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                ProjectDB project = dataSnapshot.getValue(ProjectDB.class);
                if (project == null){
//                    //remove previous fragment from the backstack, therefore navigating back
//                    getActivity().getSupportFragmentManager().popBackStack();
                }else {
                    mProject.setProjName(project.getProjName());
                    mProject.setProjFreq(project.getProjFreq());
                    mProject.setProjLength(project.getProjLength());
                    mProject.setProjLengthGoal(project.getProjLengthGoal());
                    init();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        init();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try{
            mPhotoAddListener = (OnProjectPhotoAddListener) getActivity();
        }catch(ClassCastException e){
            Log.e(TAG, "onAttach: ClassCastException " + e.getMessage() );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mProject = getProjectFromBundle();
        redefineProjectFromDatabase(mProject.getProjPhotoTag());
    }

    private Project getProjectFromBundle() {
        Log.d(TAG, "getProjectFromBundle: arguments " + getArguments());

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            return bundle.getParcelable("Project");
        }else{
            return null;
        }
    }

    private void init() {
        mProjName.setText(mProject.getProjName());
        if(mProject.getProjLength()=="1"){
            mProjLength.setText(mProject.getProjLength() +" Photo Taken");
        }else{
        mProjLength.setText(mProject.getProjLength() +" Photos Taken");
        }

        if(mProject.getProjFreq().equals("")){
            mProjFreq.setVisibility(View.GONE);
        }else{
            mProjFreq.setVisibility(View.VISIBLE);
            if (mProject.getProjFreq().equals("Hourly")) {
                mProjFreq.setText("An " + mProject.getProjFreq() + " Photo Project");
            } else {
                mProjFreq.setText("A " + mProject.getProjFreq() + " Photo Project");
            }
        }

        if(mProject.getProjLengthGoal().equals("") || mProject.getProjLengthGoal()==null){
            mProjLengthGoal.setVisibility(View.GONE);
        }else{
            mProjLengthGoal.setVisibility(View.VISIBLE);
            int dif = Integer.valueOf(mProject.getProjLengthGoal()) - Integer.valueOf(mProject.getProjLength());
            if (dif>1) {
                String difS = String.valueOf(dif);
                mProjLengthGoal.setText(difS + " Photos From Your Goal Of " + mProject.getProjLengthGoal());
            } else if (dif==1){
                String difS = String.valueOf(dif);
                mProjLengthGoal.setText(difS + " Photo From Your Goal Of " + mProject.getProjLengthGoal());
            }
            else{
                mProjLengthGoal.setText("Length Goal Met!");
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.project_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuitem_delete:
                Log.d(TAG, "onOptionsItemSelected: deleting contact");

                DeleteDialog dialog = new DeleteDialog();
                Bundle args = new Bundle();
                args.putParcelable("Project", mProject);
                dialog.setArguments(args);
                dialog.show(getFragmentManager(),"delete");
        }
        return super.onOptionsItemSelected(item);
    }

    String getGalleryPath(String projPhotoTag) {
        String folder = "LifeTimeLapse";
        String projFolder = projPhotoTag;
        File f = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath(), folder + "/" + projFolder);
        boolean directoryExists = f.exists();
        if(!directoryExists){
            Log.d(TAG, "getGalleryPath: Directory not yet existing");
            boolean isDirectoryCreated = f.mkdirs();
            Log.d(TAG, "getGalleryPath: Attempted to make directory: " + f.getAbsolutePath());
            if(isDirectoryCreated){
                Log.d(TAG, "getGalleryPath: Successful creation of directory: " + f.getAbsolutePath());
                Log.d(TAG, "getGalleryPath: Since successful, returning absolute path");
                return f.getAbsolutePath();
            }else{
                Log.d(TAG, "getGalleryPath: Failed creation of directory: " + f.getAbsolutePath());
                Log.d(TAG, "getGalleryPath: Since failed, returning Null");
                return null;
            }
        }else{
            Log.d(TAG, "getGalleryPath: directory already exists");
            return f.getAbsolutePath();
        }
    }

    private void setImageView(CircleImageView recentPic, CircleImageView firstPic, String mProjPhotoTag) {

        Log.d(TAG, "setImageView: mProjPhotoTag: " + mProjPhotoTag);

        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            recentPic.setVisibility(View.GONE);
            firstPic.setVisibility(View.GONE);
            return;
        }

        File folder = new File (getGalleryPath(mProjPhotoTag));
        //Get all images in the "folder" directory into an ArrayList of strings
        ArrayList<String> folderContentNames = new ArrayList<String>(Arrays.asList(folder.list()));
        resetProjLength(folderContentNames.size());

        ArrayList<String> folderContentNamesCopy = new ArrayList<String>(Arrays.asList(folder.list()));
        Log.d(TAG, "setImageViews: folder contents: " + folderContentNames);
        Log.d(TAG, "setImageViews: copy folder contents: " + folderContentNamesCopy);

        if(folderContentNames.size()==0){
            recentPic.setVisibility(View.GONE);
            firstPic.setVisibility(View.GONE);
        }

        else if (folderContentNames.size()==1){
            recentPic.setVisibility(View.VISIBLE);
            firstPic.setVisibility(View.GONE);
            RetrieveAndSetTwoPics(recentPic, null, folder, folderContentNames, folderContentNamesCopy);
        }

        //if(photos in file already) then sort through them to get the most recent one (highest timestamp)
        //and then set that photo as the ghostView
        else if (folderContentNames.size()>1) {
            recentPic.setVisibility(View.VISIBLE);
            firstPic.setVisibility(View.VISIBLE);
            RetrieveAndSetTwoPics(recentPic, firstPic, folder, folderContentNames, folderContentNamesCopy);
        }

    }

    private void resetProjLength(int size) {
        mProjLength.setText(String.valueOf(size) +" Photos Taken");
    }

    private void RetrieveAndSetTwoPics(CircleImageView recentPic, CircleImageView firstPic, File folder, ArrayList<String> folderContentNames, ArrayList<String> folderContentNamesCopy) {
        //remove the pic.jpg and Selfie strings from the array (the non-Copy version)
        for (int i = 0; i < folderContentNames.size(); i++) {
            if (folderContentNames.get(i).contains("pic.jpg")) {
                String entry = folderContentNames.get(i);
                String replacement = entry.replace("pic.jpg", "");
                folderContentNames.set(i, replacement);
            }
            if (folderContentNames.get(i).contains("Selfie")) {
                String entry = folderContentNames.get(i);
                String replacement = entry.replace("Selfie", "");
                folderContentNames.set(i, replacement);
            }
        }

        //Convert the array of strings to array of Longs
        Log.d(TAG, "setTwoPics: folder contents reduced to timestamp: " + folderContentNames);
        ArrayList<Long> folderContentNamesLong = new ArrayList<>(getLongArray(folderContentNames));


        //Recent Photo Set
        //Iterate through the Longs to find the maximum value, therefore the most recent one. Get that position as RecentIndex
        Long recentTimestamp = folderContentNamesLong.get(0);
        //Find the largest Long value, and set the index value
        for (int j = 1; j < folderContentNamesLong.size(); j++) {
            if (folderContentNamesLong.get(j) > recentTimestamp) {
                recentTimestamp = folderContentNamesLong.get(j);
                recentIndex = j;
            }
        }
        //go back to the original arraylist of strings, and select the index of most recent photo
        File recentFile = new File(folder.toString() + "/" + folderContentNamesCopy.get(recentIndex));
        Log.d(TAG, "setTwoPics: recentFile path: " + recentFile);
        imageLoader.displayImage("file://" + recentFile.getAbsolutePath(), recentPic);
        recentIndex=0;

        if (firstPic != null){
            //First Photo Set
            //Iterate through the Longs to find the manimum value, therefore the first. Get that position as firstIndex
            Long firstTimestamp = folderContentNamesLong.get(0);
            //Find the lowest Long value, and set the index value
            for (int i = 1; i < folderContentNamesLong.size(); i++) {
                if (folderContentNamesLong.get(i) < firstTimestamp) {
                    firstTimestamp = folderContentNamesLong.get(i);
                    firstIndex = i;
                }
            }
            //go back to the original arraylist of strings, and select the index of most recent photo
            File firstFile = new File(folder.toString() + "/" + folderContentNamesCopy.get(firstIndex));
            Log.d(TAG, "setTwoPics: firstFile path: " + firstFile);
            imageLoader.displayImage("file://" + firstFile.getAbsolutePath(), firstPic);
            firstIndex=0;
        }
    }

    /**
     * For the above string array, which contains numbers, this method converts that array to ints.
     * @param stringArray
     * @return
     */
    private ArrayList<Long> getLongArray(ArrayList<String> stringArray) {
        ArrayList<Long> result = new ArrayList<Long>();
        for(String stringValue : stringArray) {
            try {
                //Convert String to Integer, and store it into integer array list.
                result.add(Long.parseLong(stringValue));
            } catch(NumberFormatException nfe) {
                //System.out.println("Could not parse " + nfe);
                Log.w("NumberFormat", "Parsing failed! " + stringValue + " can not be an integer");
            }
        }
        return result;
    }


}
