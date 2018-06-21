package com.erikdunteman.erik.lifetimelapse.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.erikdunteman.erik.lifetimelapse.R;
import com.erikdunteman.erik.lifetimelapse.models.Project;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.nostra13.universalimageloader.core.ImageLoader.TAG;

/**
 * Created by Erik on 1/16/2018.
 */

public class ProjectsMenuCVAdapter extends ArrayAdapter<Project> {
    private LayoutInflater mInflater;
    private List<Project> mProjects = null;
    private ArrayList<Project> arrayList; //used for search bar
    private int layoutResource;
    private String mProjPhotoTag;
    private int recentIndex;
    private int firstIndex;
    private Context mContext;
    private String mAppend;

    ImageLoader imageLoader = ImageLoader.getInstance();

    public ProjectsMenuCVAdapter(@NonNull Context context, int resource, @NonNull List<Project> projects, String append) {
        super(context, resource, projects);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutResource = resource;
        this.mContext = context;
        mAppend = append;
        this.mProjects = projects;
        arrayList = new ArrayList<>();
        this.arrayList.addAll(mProjects);
    }

    private static class ViewHolder{
        //Stuff to change if using this as template ******************
        CircleImageView firstPic;
        CircleImageView recentPic;
        TextView projectName;
        //ProgressBar mProgressBar;
        //*******************************************************************
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        //ViewHolder Build Pattern Start ******************
        final ViewHolder holder;

        if(convertView == null){
            convertView = mInflater.inflate(layoutResource, parent, false);
            holder = new ViewHolder();

            //************************************Stuff to Change if using this as template******
            holder.projectName = convertView.findViewById(R.id.cvProjectName);
            holder.firstPic = convertView.findViewById(R.id.projectImage1);
            holder.recentPic = convertView.findViewById(R.id.projectImage2);
            //holder.mProgressBar = (ProgressBar) convertView.findViewById(R.id.projectProgressBar);
            //**********************************************************************

            convertView.setTag(holder);
        }
        else{
            holder = (ViewHolder) convertView.getTag();
        }

        //************************************Stuff to Change if using this as template******
        try {
            String projects = getItem(position).getProjName();
            holder.projectName.setText(projects);
            String mProjPhotoTag = getItem(position).getProjPhotoTag();
            //initialize the first and recent photos into their locations
            setImageView(holder.recentPic, holder.firstPic, mProjPhotoTag);
        }catch(Exception e) {
            e.printStackTrace();
        }
        return convertView;
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

        Log.d(TAG, "setImageView: Loading Images into their Spots. Started Tag: " + mProjPhotoTag);
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            recentPic.setVisibility(View.GONE);
            firstPic.setVisibility(View.GONE);
            return;
        }

        File folder = new File (getGalleryPath(mProjPhotoTag));
        //Get all images in the "folder" directory into an ArrayList of strings
        ArrayList<String> folderContentNames = new ArrayList<String>(Arrays.asList(folder.list()));
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
            firstIndex = 0;
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

    // Search bar filter class
    public void filter(String characterText){

        characterText = characterText.toLowerCase(Locale.getDefault());
        mProjects.clear();
        if(characterText.length() == 0){
            mProjects.addAll(arrayList);
        }else{
            mProjects.clear();
            for(Project project: arrayList){
                if(project.getProjName().toLowerCase(Locale.getDefault()).contains(characterText)){
                    mProjects.add(project);
                }
            }
        }
        notifyDataSetChanged();
    }


}

//