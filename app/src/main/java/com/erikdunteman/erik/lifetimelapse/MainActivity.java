package com.erikdunteman.erik.lifetimelapse;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.InterstitialAd;


import com.erikdunteman.erik.lifetimelapse.models.Project;
import com.erikdunteman.erik.lifetimelapse.utils.UniversalImageLoader;
import com.nostra13.universalimageloader.core.ImageLoader;

public class MainActivity extends AppCompatActivity implements
        ProjectsMenuFragment.OnProjectSelectedListener,
        ProjectInfoFragment.OnProjectPhotoAddListener,
        //Step 3 for sending info from Projects Menu Fragment to Project Add Fragment
        ProjectsMenuFragment.OnProjectAddListener{

    private static final String TAG = "MainActivity";

    private InterstitialAd mInterstitialAd;

    //Navigate from project menu to project info fragment, carrying selected project in bundle
    @Override
    public void OnProjectSelected(Project project) {
        Log.d(TAG, "OnProjectSelected: project selected from " + getString(R.string.projects_menu_fragment) + " " + project.getProjName());
        ProjectInfoFragment fragment = new ProjectInfoFragment();
        Bundle args = new Bundle();
        args.putParcelable(getString(R.string.project),project);
        fragment.setArguments(args);

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(getString(R.string.project_info_fragment));
        transaction.commit();
    }

    //Navigate from project info to camera fragment, carrying selected project in bundle
    @Override
    public void OnProjectPhotoAdd(Project project) {
        Log.d(TAG, "OnProjectPhotoAdd: Navigate to camera to add photo into LifeTimeLapse folder: " + project.getProjPhotoTag());
        CameraFragment fragment = new CameraFragment();
        Bundle args = new Bundle();
        args.putParcelable(getString(R.string.project),project);
        fragment.setArguments(args);

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack("ProjectsMenuFragment");
        transaction.commit();
    }


    //Step 4 for sending info from Projects Menu Fragment to Project Add Fragment
    @Override
    public void onProjectAdd() {
        Log.d(TAG, "OnProjectAdd: Navigate to projects add");
        ProjectAddFragment fragment = new ProjectAddFragment();
        //Bundle only needed if passing info. Here, we are simply navigating to the add fragment. No need to carry info.
//        Bundle args = new Bundle();
//        args.putParcelable(getString(R.string.project),project);
//        fragment.setArguments(args);
        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack("ProjectMenuFragment");
        transaction.commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: started");
        MobileAds.initialize(this, "ca-app-pub-7411991967781156~6815414311");

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-7411991967781156/3945353173");
        AdRequest request = new AdRequest.Builder()
//                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        mInterstitialAd.loadAd(request);

        init_f_w1();
        initImageLoader();
    }

    private void initImageLoader() {
        UniversalImageLoader universalImageLoader = new UniversalImageLoader(MainActivity.this);
        ImageLoader.getInstance().init(universalImageLoader.getConfig());
    }

    //Initialize the Intro Screen
    private void init_f_w1() {
        WelcomeOneFragment fragment = new WelcomeOneFragment();
        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }



}



