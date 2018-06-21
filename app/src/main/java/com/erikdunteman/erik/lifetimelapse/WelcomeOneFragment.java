package com.erikdunteman.erik.lifetimelapse;

import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

/**
 * Created by Erik on 1/19/2018.
 */

public class WelcomeOneFragment extends Fragment {
    private static final String TAG = "WelcomeOneFragment";

    private static final int REQUEST_STORAGE_PERMISSIONS = 2;
    ImageView welcomeImage;
    ViewGroup vgContainer;
    ViewGroup container;
    int screenHeight;
    int screenWidth;
    View view;
    @Nullable
    @Override
    public View getView() {
        return view;
    }
    public void setView(View view) {
        this.view = view;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_welcome, container, false);
        setView(view);
        Log.d(TAG, "onCreateView: Welcome Screen 1 Opened");

        final ViewGroup vgContainer = container;
        final ImageView welcomeImage = view.findViewById(R.id.welcomeImage);
        welcomeImage.setImageResource(R.drawable.camera_on_wood_cornercrop);


        RelativeLayout screen = view.findViewById(R.id.welcomeScreen);
        screen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Welcome Screen Clicked");
                init_f_li();
            }
        });

        return view;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        view = getView();
        int Orientation = view.getResources().getConfiguration().orientation;
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfiguration Changed to: " + Orientation);
        final ImageView welcomeImage = view.findViewById(R.id.welcomeImage);
        if (Orientation == Configuration.ORIENTATION_LANDSCAPE){
            welcomeImage.setImageResource(R.drawable.camera_on_wood_cornercrop_landscape);
        } else {
            welcomeImage.setImageResource(R.drawable.camera_on_wood_cornercrop);
        }
    }

    //initialize the first fragment (ProjectsMenuFragment)
    public void init_f_li(){

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)){
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSIONS);
            } else {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSIONS);
            }
        }else { //Permission was granted
            ProjectsMenuFragment fragment = new ProjectsMenuFragment();
            android.support.v4.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
            // replace whatever is in the fragment_container view with this fragment,
            // and add the transaction to the back stack so the user can navigate back
            transaction.replace(R.id.fragment_container, fragment);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.commit();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSIONS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ProjectsMenuFragment fragment = new ProjectsMenuFragment();
                    android.support.v4.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    // replace whatever is in the fragment_container view with this fragment,
                    // and add the transaction to the back stack so the user can navigate back
                    transaction.replace(R.id.fragment_container, fragment);
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                    transaction.commit();

                } else {
                    Toast.makeText(getActivity(), "LifeTimeLapse Must Interact With Image Storage To Run.",Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }
}
