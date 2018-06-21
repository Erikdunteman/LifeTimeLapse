package com.erikdunteman.erik.lifetimelapse;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.erikdunteman.erik.lifetimelapse.utils.Delay;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Erik on 1/19/2018.
 */

public class ProjectProgressFragment extends Fragment {
    private static final String TAG = "WelcomeOneFragment";


    private android.support.v7.widget.Toolbar toolbar;
    private ImageView preview;
    private ArrayList<String> folderContentNames;
    private ArrayList<Bitmap> loadedBitmaps;
    private int index;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_projectprogress, container, false);
        Log.d(TAG, "onCreateView: ProjectProgress Opened");

        //required for setting up the tool bar
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        toolbar = view.findViewById(R.id.projectPreviewToolbar);
        preview = view.findViewById(R.id.previewFrame);

        ImageView playVid = view.findViewById(R.id.playVid);
        playVid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Runslideshow(getGalleryPath());
            }
        });

        Log.d(TAG, "onCreateView: preview exists:");
        return view;
    }


    /**
     * A for funzies method to make the slideshow and simply stream it through the ghostView
     */
    public void Runslideshow(String galleryPath) {
        Log.d(TAG, "Runslideshow: Attempting to run slideshow");

        File folder = new File(galleryPath);
//Get all images in the "folder" directory into an ArrayList of strings
        folderContentNames = new ArrayList<String>(Arrays.asList(folder.list()));
        Log.d(TAG, "Runslideshow: folderContentNames.size = " + folderContentNames.size());
        loadedBitmaps = new ArrayList<Bitmap>();
        for (int i = 0; i < folderContentNames.size(); i++) {
            index = i;
            Delay.delay(500, new Delay.DelayCallback() {
                @Override
                public void afterDelay() {
                    String fileString = getGalleryPath() + "/" + folderContentNames.get(index);
                    Bitmap slideBitmap = BitmapFactory.decodeFile(fileString);
                    Log.d(TAG, "slideBitmap: " + index + "  " +slideBitmap);
                    loadedBitmaps.add(slideBitmap);
                }

            });
        }
        for (int i = 0; i < loadedBitmaps.size(); i++) {
            index = i;
            Delay.delay(500, new Delay.DelayCallback() {
                @Override
                public void afterDelay() {
                    //set the bitmap to ghostView
                    preview.setImageBitmap(loadedBitmaps.get(index));
                }
            });
        }
    }


    public String getGalleryPath() {
        String folder = "LifeTimeLapse";
        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath(), folder);
        boolean directoryExists = f.exists();
        if (!directoryExists) {
            Log.d(TAG, "getGalleryPath: Directory not yet existing");
            boolean isDirectoryCreated = f.mkdirs();
            Log.d(TAG, "getGalleryPath: Attempted to make directory: " + f.getAbsolutePath());
            if (isDirectoryCreated) {
                Log.d(TAG, "getGalleryPath: Successful creation of directory: " + f.getAbsolutePath());
                Log.d(TAG, "getGalleryPath: Since successful, returning absolute path");
                return f.getAbsolutePath();
            } else {
                Log.d(TAG, "getGalleryPath: Failed creation of directory: " + f.getAbsolutePath());
                Log.d(TAG, "getGalleryPath: Since failed, returning Null");
                return null;
            }
        } else {
            Log.d(TAG, "getGalleryPath: directory already exists");
            return f.getAbsolutePath();
        }
    }
}