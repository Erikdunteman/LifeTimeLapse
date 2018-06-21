/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.erikdunteman.erik.lifetimelapse;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.erikdunteman.erik.lifetimelapse.models.Project;
import com.erikdunteman.erik.lifetimelapse.models.ProjectDB;
import com.erikdunteman.erik.lifetimelapse.utils.AutoFitTextureView;
import com.erikdunteman.erik.lifetimelapse.utils.Delay;
import com.erikdunteman.erik.lifetimelapse.utils.ContextGetter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.google.android.gms.internal.zzahn.runOnUiThread;

public class CameraFragment extends Fragment
        implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {


    //This will evade the nullpointer exception when adding to a new bundle from MainActivity.
    public CameraFragment() {
        super();
        setArguments(new Bundle());
    }

    /**
     * ____________________________________________________________________________________________
     * ____________________________________________________________________________________________
     *
     * Variable Declaration
     * ____________________________________________________________________________________________
     * ____________________________________________________________________________________________
     */

    /**
     * Project, for recieving project from bundle
     */
    private Project mProject;


    //Firebase Auth
    private FirebaseAuth.AuthStateListener mAuthListener;


    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_STORAGE_PERMISSIONS = 2;

    //Photo compression threasholds and variables
    private static final double MB_THRESHHOLD = 5.0;
    private static final double MB = 1000000.0;
    private byte[] mBytes;
    private double progress;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "CameraFragment";

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /**
     * Project Length
     */
    private String mProjectLength;

    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId;

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

    /**
     * A Toast item, for whatever reason I seem to need it...
     */
    private Toast mToast;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;



    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private int mState = STATE_PREVIEW;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Whether the current camera device supports Flash or not.
     */
    private boolean mFlashSupported;

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;



    /**
     * ____________________________________________________________________________________________
     * ____________________________________________________________________________________________
     *
     * Variable Declaration - With Getters and Setters
     * ____________________________________________________________________________________________
     * ____________________________________________________________________________________________
     */


    /**
     * Project Tag to Create individual project folders
     */
    private String mProjPhotoTag;
    public String getmProjPhotoTag() {
        return mProjPhotoTag;
    }
    public void setmProjPhotoTag(String mProjPhotoTag) {
        this.mProjPhotoTag = mProjPhotoTag;
    }


    /**
     * Unique photo name for placement within the mProjPhotoTag folders;
     */
    public String getmNewPicStringName() {
        return mNewPicStringName;
    }
    public void setmNewPicStringName(String mNewPicStringName) {
        this.mNewPicStringName = mNewPicStringName;
    }
    private String mNewPicStringName;


    /**
     * Image for saving reference
     */
    private Image mImage;
    public Image getmImage() {
        return mImage;
    }
    public void setmImage(Image mImage) {
        this.mImage = mImage;
    }

    /**
     * Carry the cameraId to call for Textureview Display and capturing.
     * (Is the camera forward facing or backward?)
     */
    public String cameraIdSelected = "0";
    public String getCameraIdSelected() {
        return cameraIdSelected;
    }
    public void setCameraIdSelected(String cameraIdSelected) {
        this.cameraIdSelected = cameraIdSelected;
    }

    /**
     * Facing direction of selected camera
     */
    public int facing;
    public int getFacing() {
        return facing;
    }
    public void setFacing(int facing) {
        this.facing = facing;
    }

    /**
     * This is the ArrayList containing all photo files
     */
    private ArrayList<String> mFolderContentNames;
    public ArrayList<String> getmFolderContentNames() {
        return mFolderContentNames;
    }
    public void setmFolderContentNames(ArrayList<String> mFolderContentNames) {
        this.mFolderContentNames = mFolderContentNames;
    }

    /**
     * This is the output file for our picture.
     */
    private ImageView mGhostView;
    public ImageView getmGhostView() {
        return mGhostView;
    }
    public void setmGhostView(ImageView mGhostView) {
        this.mGhostView = mGhostView;
    }

    /**
     * This is the output file for our picture.
     */
    private File mNewPicFile;
    public File getmNewPicFile() {
        return mNewPicFile;
    }
    public void setmNewPicFile(File mNewPicFile) {
        this.mNewPicFile = mNewPicFile;
    }


    /**
     * This is the index in the directory of the most recent photo
     */
    public int recentIndex;
    public int getRecentIndex() {
        return recentIndex;
    }
    public void setRecentIndex(int recentIndex) {
        this.recentIndex = recentIndex;
    }


    /**
     * This is photo loading progress bar
     */
    public ProgressBar mProgressBar;
    public ProgressBar getmProgressBar() {
        return mProgressBar;
    }
    public void setmProgressBar(ProgressBar mProgressBar) {
        this.mProgressBar = mProgressBar;
    }

    /**
     * ____________________________________________________________________________________________
     * ____________________________________________________________________________________________
     *
     * Variable Declaration - With Overrides
     * ____________________________________________________________________________________________
     * ____________________________________________________________________________________________
     */
    /**
    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height, getCameraIdSelected());

            //Adjusting the Ghost Image alpha to correspond with slider
            final SeekBar ghostSlider = getView().findViewById(R.id.alphaSlider);

            //With camera open, initiate ghostView
            final ImageView ghostView = getView().findViewById(R.id.ghost);
            //initiate and hide the ghostViewFirstShot
            final TextView ghostViewFirstShot = getView().findViewById(R.id.ghostfirst);
            ghostViewFirstShot.setVisibility(View.GONE);
            //initiate and hide the ghostViewSecondShot
            final TextView ghostViewSecondShot = getView().findViewById(R.id.ghostsecond);
            ghostViewSecondShot.setVisibility(View.GONE);
            setGhostView(ghostView, ghostViewFirstShot, ghostViewSecondShot, ghostSlider);


            ghostSlider.setMax(200);
            ghostSlider.setProgress(100);
            ghostSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean userInteraction) {
                    if (userInteraction) {
                        //ghostSlider.setProgress(progress);
                        Log.d(TAG, "onProgressChanged: progress value: " + progress);
                        ghostView.setImageAlpha(progress + 55);
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            Log.d(TAG, "onSurfaceTextureAvailable: opening camera" + getCameraIdSelected());
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransformUniversal(width, height, mTextureView, mPreviewSize);
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }
        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }
    };

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {


            Image image = reader.acquireNextImage();
//            Log.d(TAG, "onImageAvailable: image = null: " + image.equals(null));
            setmImage(image);

            mBackgroundHandler.post(new ImageSaver(image, mNewPicFile, mProjPhotoTag, mNewPicStringName));


            //Upload to FireBase cloud for safekeeping
            Log.d(TAG, "run: Beginning photo upload to FireBase");

//            ByteBuffer buffer = getmImage().getPlanes()[0].getBuffer();
//            byte[] bytes = new byte[buffer.remaining()];
//            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
//            uploadNewPhoto(bitmap,mNewPicFile);

            /**
             * A purely aesthetic post-snap delay, then navigate back to project info screen
             */
            Delay.delay(800, new Delay.DelayCallback() {
                @Override
                public void afterDelay() {


                    //remove previous fragment from the backstack, therefore navigating back
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }
    };


    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    //Log.d(TAG, "process: STATE_PREVIEW");
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK: {
                    //Log.d(TAG, "process: STATE_WAITING_LOCK");
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    //Log.d(TAG, "process: STATE_WAITING_PRECAPTURE");
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    //Log.d(TAG, "process: STATE_WAITING_NON_PRECAPTURE");
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
            //Log.d(TAG, "onCaptureCompleted: Capture completed");
        }
    };

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                    option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    
    public static CameraFragment newInstance() {
        return new CameraFragment();
    }


    /**
     * ____________________________________________________________________________________________
     * ____________________________________________________________________________________________
     *
     * Activity Lifecycle Overrides
     * ____________________________________________________________________________________________
     * ____________________________________________________________________________________________
     */
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        view.requestFocus();
        mProject = getProjectFromBundle();
        setupFirebaseAuth();
        Log.d(TAG, "onCreateView: OnCreateView Initiated, fragement_camera2_basic inflated into container");
        
        if (mProject != null) {
            Log.d(TAG, "onCreateView: recieved project:" + mProject.getProjName());
        } else {
            Log.d(TAG, "onCreateView: project not recieved");
        }

        setmProjPhotoTag(mProject.getProjPhotoTag());


        //OnClickListener for camera selection (forward, backward, external)
        final ImageView camSwap = view.findViewById(R.id.camSwap);
        setCameraIcon(getFacing(), camSwap);
        camSwap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                swapCamera(getCameraIdSelected());
            }
        });

        //OnClickListener for take photo
        final ImageView capture = view.findViewById(R.id.capture);
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onCreateView: Capture Button Pressed");
                final ImageView captureEffect = getView().findViewById(R.id.captureEffect);
                captureEffect.setVisibility(View.VISIBLE);
                Delay.delay(400, new Delay.DelayCallback() {
                    @Override
                    public void afterDelay() {
                        captureEffect.setVisibility(View.GONE);
                    }
                });
                takePicture();
            }
        });

//        //Attempt to set up Key Listener for taking photo
//        view.setOnKeyListener(new View.OnKeyListener() {
//            @Override
//            public boolean onKey(View v, int keyCode, KeyEvent event) {
//                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
//                    Log.d(TAG, "onKey: volume event");
//                    takePicture();
//                    return true;
//                } else {
//                    return false;
//                }
//            }
//        });

        return view;
    }



    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mTextureView = view.findViewById(R.id.texture);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
            }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        checkAuthenticationState();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight(),getCameraIdSelected());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

    }

    private void checkAuthenticationState(){
        Log.d(TAG, "checkAuthenticationState: checking authentication state.");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if(user == null){
            Log.d(TAG, "checkAuthenticationState: user is null, navigating back to login screen.");
            LoginFragment fragment = new LoginFragment();
            android.support.v4.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
            // replace whatever is in the fragment_container view with this fragment,
            // and add the transaction to the back stack so the user can navigate back
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }else{
            Log.d(TAG, "checkAuthenticationState: user is authenticated.");
        }
    }


    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }




    
    /**
     * ____________________________________________________________________________________________
     * ____________________________________________________________________________________________
     *
     * Set Up Camera Outputs
     * ____________________________________________________________________________________________
     * ____________________________________________________________________________________________
     */
    
    /**
     * Sets up member variables related to camera.
     *     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private void setUpCameraOutputs(int width, int height, String cameraIdSelected) {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);


        //Loop to List Camera Options
        try {
            for (String cameraIdloop : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraIdloop);

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    Log.d(TAG, "setUpCameraOutputs: Loop cameraId: " + cameraIdloop + " has null map");
                    continue;
                }

                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                //---Erik adding Log to See Camera Ids and their respective orientations
                Log.d(TAG, "setUpCameraOutputs: Loop cameraId: " + cameraIdloop + "     Facing Value: " + facing + "   Where facing = 0 is screenside (Front), 1 is Backside, 2 is External");
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
//            ErrorDialog.newInstance(getString(R.string.camera_error))
//                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
        
        try {
            CameraCharacteristics characteristics
                    = manager.getCameraCharacteristics(getCameraIdSelected());

            setFacing(characteristics.get(CameraCharacteristics.LENS_FACING));

            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            // For still image captures, we use the largest available size.
            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new CompareSizesByArea());
            mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                    ImageFormat.JPEG, /*maxImages*/2);
            mImageReader.setOnImageAvailableListener(
                    mOnImageAvailableListener, mBackgroundHandler);

            // Find out if we need to swap dimension to get the preview size relative to sensor
            // coordinate.
            int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            //noinspection ConstantConditions
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            boolean swappedDimensions = false;
            switch (displayRotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                    if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                        swappedDimensions = true;
                    }
                    break;
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                        swappedDimensions = true;
                    }
                    break;
                default:
                    Log.e(TAG, "Display rotation is invalid: " + displayRotation);
            }

            Point displaySize = new Point();
            activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
            int rotatedPreviewWidth = width;
            int rotatedPreviewHeight = height;
            int maxPreviewWidth = displaySize.x;
            int maxPreviewHeight = displaySize.y;

            if (swappedDimensions) {
                rotatedPreviewWidth = height;
                rotatedPreviewHeight = width;
                maxPreviewWidth = displaySize.y;
                maxPreviewHeight = displaySize.x;
            }

            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                maxPreviewWidth = MAX_PREVIEW_WIDTH;
            }

            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                maxPreviewHeight = MAX_PREVIEW_HEIGHT;
            }

            // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
            // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
            // garbage capture data.
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                    maxPreviewHeight, largest);

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(
                        mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(
                        mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }

            // Check if the flash is supported.
            Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            mFlashSupported = available == null ? false : available;

            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            setFacing(facing);
            Log.d(TAG, "setUpCameraOutputs: New Facing: " + getFacing());
            final ImageView camSwap = getView().findViewById(R.id.camSwap);
            setCameraIcon(facing, camSwap);
            mCameraId = cameraIdSelected;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
//            ErrorDialog.newInstance(getString(R.string.camera_error))
//                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }

    }

    /**
     * ____________________________________________________________________________________________
     * ____________________________________________________________________________________________
     *
     * Custom Methods
     * ____________________________________________________________________________________________
     * ____________________________________________________________________________________________
     */



    public void requestCameraPermission() {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }
    public void requestStorageWritePermission() {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSIONS);
    }
    public void requestStorageReadPermission() {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

        if (requestCode == REQUEST_STORAGE_PERMISSIONS) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Set Up GhostView
     */
    private void setGhostView(ImageView ghostView, TextView ghostViewFirstShot, TextView ghostViewSecondShot, SeekBar ghostSlider) {

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestStorageWritePermission();
            return;
        }
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestStorageReadPermission();
            return;
        }
        File folder = new File (getGalleryPath(getmProjPhotoTag()));
        //Get all images in the "folder" directory into an ArrayList of strings
        ArrayList<String> folderContentNames = new ArrayList<String>(Arrays.asList(folder.list()));
        setmFolderContentNames(folderContentNames);
        ArrayList<String> folderContentNamesCopy = new ArrayList<String>(Arrays.asList(folder.list()));
        Log.d(TAG, "setGhostView: folder contents: " + folderContentNames);

        if(folderContentNames.size()==0){
            ghostView.setVisibility(View.GONE);
            ghostViewFirstShot.setVisibility(View.VISIBLE);
            ghostSlider.setVisibility(View.GONE);
        }

        else if (folderContentNames.size()==1){
            ghostViewSecondShot.setVisibility(View.VISIBLE);
            RetrieveAndSetGhostView(ghostView, folder, folderContentNames, folderContentNamesCopy);
            ghostSlider.setVisibility(View.VISIBLE);
        }

        //if(photos in file already) then sort through them to get the most recent one (highest timestamp)
        //and then set that photo as the ghostView
        else if (folderContentNames.size()>1) {
            RetrieveAndSetGhostView(ghostView, folder, folderContentNames, folderContentNamesCopy);
            ghostSlider.setVisibility(View.VISIBLE);
        }


    }

    private void RetrieveAndSetGhostView(ImageView ghostView, File folder, ArrayList<String> folderContentNames, ArrayList<String> folderContentNamesCopy) {
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
        Log.d(TAG, "setGhostView: folder contents reduced to timestamp: " + folderContentNames);
        ArrayList<Long> folderContentNamesLong = new ArrayList<>(getLongArray(folderContentNames));

        //Iterate through the Longs to find the maximum value, therefore the most recent one. Get that position as RecentIndex
        Long recentTimestamp = folderContentNamesLong.get(0);
        //Find the largest Long value, and set the index value
        for (int i = 1; i < folderContentNamesLong.size(); i++) {
            if (folderContentNamesLong.get(i) > recentTimestamp) {
                recentTimestamp = folderContentNamesLong.get(i);
                setRecentIndex(i);
            }
        }
        //go back to the original arraylist of strings, and select the index of most recent photo
        File recentFile = new File(folder.toString() + "/" + folderContentNamesCopy.get(getRecentIndex()));
        Log.d(TAG, "setGhostView: recentFile path: " + recentFile);
        //If image is from the frontside (screenside) photo, it will carry the name "Selfie" and will
        //require mirroring in order to be displayed correctly with the ghostView
        if (recentFile.getAbsolutePath().contains("Selfie")) {
            //We need to mirror the bitmap and then set to ghostView
            //Convert file to Bitmap
            Bitmap recentBitmap = BitmapFactory.decodeFile(recentFile.getAbsolutePath());
            Log.d(TAG, "setGhostView: new ghostView recentBitmap Dimensions: Width x Height " + recentBitmap.getWidth() + " x " + recentBitmap.getHeight());
            Bitmap recentBitmapMirrorOut;
            Matrix matrix = new Matrix();
            matrix.preScale(-1.0f, 1.0f);
            recentBitmapMirrorOut = Bitmap.createBitmap(recentBitmap, 0, 0, recentBitmap.getWidth(), recentBitmap.getHeight(), matrix, true);
            //set the mirrored bitmap to ghostView
            Log.d(TAG, "setGhostView: new ghostView recentBitmapOut Dimensions: Width x Height " + recentBitmapMirrorOut.getWidth() + " x " + recentBitmapMirrorOut.getHeight());
            ghostView.setImageBitmap(recentBitmapMirrorOut);
            Log.d(TAG, "setGhostView: new ghostView ghostView Dimensions: Width x Height " + recentBitmapMirrorOut.getWidth() + " x " + recentBitmapMirrorOut.getHeight());
            //Realign with top
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) ghostView.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            ghostView.setLayoutParams(params); //causes layout update
        } else {
            //set the non-mirrored bitmap to ghostView
            //Convert file to Bitmap
            Bitmap recentBitmap = BitmapFactory.decodeFile(recentFile.getAbsolutePath());
            Log.d(TAG, "setGhostView: new ghostView recentBitmap Dimensions: Width x Height " + recentBitmap.getWidth() + " x " + recentBitmap.getHeight());
            Bitmap recentBitmapOut;
            Matrix matrix = new Matrix();
            recentBitmapOut = Bitmap.createBitmap(recentBitmap, 0, 0, recentBitmap.getWidth(), recentBitmap.getHeight(), matrix, true);
            //set the mirrored bitmap to ghostView
            Log.d(TAG, "setGhostView: new ghostView recentBitmapOut Dimensions: Width x Height " + recentBitmapOut.getWidth() + " x " + recentBitmapOut.getHeight());
            ghostView.setImageBitmap(recentBitmapOut);
            Log.d(TAG, "setGhostView: new ghostView ghostView Dimensions: Width x Height " + recentBitmapOut.getWidth() + " x " + recentBitmapOut.getHeight());
            //Realign with top
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) ghostView.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            ghostView.setLayoutParams(params); //causes layout update
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

    /**
     * Changes the camera direction
     */
    private void swapCamera(String cameraIdSelected) {
        closeCamera();
        //Test if cameraIdSelection is below phone's limits (usually 0 for back facing, 1 for front facing, 2 if external camera)
        //This assumes that cameraId can be max 1, until some logic can be written to count available cameras and
        //set that as the new max limit.
        if(Integer.valueOf(cameraIdSelected) < 1){
            Integer cameraIdSelectedInt = (Integer.valueOf(cameraIdSelected)+1);
            setCameraIdSelected(cameraIdSelectedInt.toString());
        }else{setCameraIdSelected("0");
        }
        Log.d(TAG, "swapCamera: new camperaIdSelected: " + getCameraIdSelected());
        openCamera(mTextureView.getWidth(), mTextureView.getHeight(), getCameraIdSelected());
    }

    /**
     * Changes the icon
     * @param facing
     * @param camSwap
     */
    private void setCameraIcon(int facing, ImageView camSwap) {
        if(facing==0){
            camSwap.setImageResource(R.drawable.ic_to_camera_rear);
        }else if (facing ==1){
            camSwap.setImageResource(R.drawable.ic_to_camera_front);
        } else {
            camSwap.setImageResource(R.drawable.ic_cam_swap);
        }
    }

    /**
     * Opens the camera specified by {@link CameraFragment#mCameraId}.
     */
    private void openCamera(int width, int height, String cameraIdSelected) {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }
        setUpCameraOutputs(width, height, cameraIdSelected);
        configureTransformUniversal(width, height, mTextureView, mPreviewSize);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }




    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Log.d(TAG, "createCameraPreviewSession: mPreviewSize, Width x Height:   " + mPreviewSize.getHeight() + " x " + mPreviewSize.getWidth());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            Log.d(TAG, "createCameraPreviewSession: exception thrown");
            e.printStackTrace();
        }
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransformUniversal(int viewWidth, int viewHeight, View view, Size size) {
        Activity activity = getActivity();
        if (null == view || null == size || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, size.getHeight(), size.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / size.getHeight(),
                    (float) viewWidth / size.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * Initiate a still image capture.
     */
    public void takePicture() {
        Log.d(TAG, "takePicture: ");
        try{
            //Setting mNewPicFile to file name, depending on camera orientation
            facing = getFacing();
            String projectFolderPath = getGalleryPath(getmProjPhotoTag());
            int currentProjectSize = getmFolderContentNames().size();
            int saveIndex = currentProjectSize + 1;
            String index;
            if (saveIndex<=9){
                index = "000"+String.valueOf(saveIndex);
            } else if (saveIndex>=10 && saveIndex<=99){
                index = "00"+String.valueOf(saveIndex);
            } else if (saveIndex>=100 && saveIndex<=999) {
                index = "0" + String.valueOf(saveIndex);
            } else {
                index = String.valueOf(saveIndex);
            }

            if(facing==0){ //It is a selfie - mark file as such
                File NewPicFile = new File(projectFolderPath, index + "Selfiepic.jpg");
                setmNewPicFile(NewPicFile);
                setmNewPicStringName(index +"Selfiepic.jpg");
            }else { //It is not a selfie, leave file marking basic
                File NewPicFile = new File(getGalleryPath(getmProjPhotoTag()), index + "pic.jpg");
                setmNewPicFile(NewPicFile);
                setmNewPicStringName(index +"pic.jpg");
            }
            Log.d(TAG, "takePicture: NewPicFile = " + getmNewPicFile());
        } catch(Exception e) {
            e.printStackTrace();
        }


        lockFocus();
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        Log.d(TAG, "lockFocus: ");
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence() {
        Log.d(TAG, "runPrecaptureSequence: ");
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        Log.d(TAG, "captureStillPicture: ");
        try {
            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    showToast("Saved to Phone");
                    Log.d(TAG, "Saved to: " + mNewPicFile.toString());
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }


    @Override
    public void onClick(View view) {
    }



    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {
        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        private final String ProjPhotoTag;
        private  final String NewPicStringName;
        Context context = ContextGetter.getAppContext();

        ImageSaver(Image image, File file, String mProjPhotoTag, String mNewPicStringName) {

            mImage = image;
            mFile = file;
            ProjPhotoTag = mProjPhotoTag;
            NewPicStringName = mNewPicStringName;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);


                MediaScannerConnection.scanFile(context, new String[] {mFile.getAbsolutePath()} , null, null);


                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                CameraFragment cameraFragment = new CameraFragment();
                cameraFragment.uploadNewPhoto(bitmap, ProjPhotoTag, NewPicStringName);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }


    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

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


    private void uploadNewPhoto(Bitmap imageBitmap, String mProjPhotoTag, String mNewPicStringName) {
        Log.d(TAG, "uploadNewPhoto: uploading new photo to firebase storage.");
        //Only accept image sizes that are compressed to under 5MB. If thats not possible
        //then do not allow image to be uploaded
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        BackgroundImageResize resize = new BackgroundImageResize(imageBitmap, mProjPhotoTag, mNewPicStringName);
        Uri uri = null;
        resize.execute(uri);
    }

    /**
     * 1) doinBackground takes an imageUri and returns the byte array after compression
     * 2) onPostExecute will print the % compression to the log once finished
     */
    public class BackgroundImageResize extends AsyncTask<Uri, Integer, byte[]> {

        Bitmap mBitmap;
        String mProjPhotoTag;
        String mNewPicStringName;

        public BackgroundImageResize(Bitmap bm, String photoTag, String photoName) {
            if(bm != null){
                mBitmap = bm;
            }
            mProjPhotoTag=photoTag;
            mNewPicStringName=photoName;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog();
            showToast("compressing image");
        }

        @Override
        protected byte[] doInBackground(Uri... params ) {
            Log.d(TAG, "doInBackground: started.");

            if(mBitmap == null){

                try {
                    mBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), params[0]);
                    Log.d(TAG, "doInBackground: bitmap size: megabytes: " + mBitmap.getByteCount()/MB + " MB");
                } catch (IOException e) {
                    Log.e(TAG, "doInBackground: IOException: ", e.getCause());
                }
            }

            byte[] bytes = null;
            for (int i = 1; i < 11; i++){
                if(i == 10){
                    showToast( "That image is too large.");
                    break;
                }
                bytes = getBytesFromBitmap(mBitmap,100/i);
                Log.d(TAG, "doInBackground: megabytes: (" + (11-i) + "0%) "  + bytes.length/MB + " MB");
                if(bytes.length/MB  < MB_THRESHHOLD){
                    return bytes;
                }
            }
            return bytes;
        }


        @Override
        protected void onPostExecute(byte[] bytes) {
            super.onPostExecute(bytes);
            hideDialog();
            mBytes = bytes;
            //execute the upload
            executeUploadTask(mProjPhotoTag, mNewPicStringName);

            updateLength(mProjPhotoTag);
        }
    }

    private void updateLength(String mProjPhotoTag) {
        //Update the project length
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("projects")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(mProjPhotoTag);
        //Get current length
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ProjectDB project = dataSnapshot.getValue(ProjectDB.class);
                Log.d(TAG, "onDataChange: " + project.toString());
                mProjectLength = project.getProjLength();
                Log.d(TAG, "onDataChange: previous mProjectLength: " + mProjectLength);
                int mProjectLengthInt = Integer.valueOf(mProjectLength);
                //add one to that length and then set it
                String mNewProjectLength = String.valueOf(mProjectLengthInt + 1);
                Log.d(TAG, "onDataChange: mNewProjectLength: " + mNewProjectLength);
                ref.child("projLength").setValue(mNewProjectLength);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    // convert from bitmap to byte array
    public static byte[] getBytesFromBitmap(Bitmap bitmap, int quality) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        return stream.toByteArray();
    }

    private void executeUploadTask(final String mProjPhotoTag, final String mNewPicStringName){
        showDialog();

        //Add photo timestamp to realtime database
        //Get all photoTags in that project
        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("projects")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(mProjPhotoTag).child("photoNames");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: entered");
                Log.d(TAG, "onDataChange: datasnapshot: " + dataSnapshot.toString());

                GenericTypeIndicator<ArrayList<String>> t = new GenericTypeIndicator<ArrayList<String>>() {};
                ArrayList<String> photoTags = dataSnapshot.getValue(t);
                if (photoTags != null) {
                    Log.d(TAG, "onDataChange: old photoTags: " + photoTags.toString());
                }else {
                    photoTags = new ArrayList<String>();
                    Log.d(TAG, "onDataChange: No old photoTags");
                }
                //add the new photoTag to the arraylist, then update the photoNames node
                String photoName = mNewPicStringName;
                photoTags.add(photoName);
                Log.d(TAG, "onDataChange: new photoTags: " + photoTags.toString());
                databaseReference.setValue(photoTags);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });



        //specify where the photo will be stored
        final StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(mProjPhotoTag)
                .child(mNewPicStringName);

        if(mBytes.length/MB < MB_THRESHHOLD) {

            //if the image size is valid then we can submit to database
            UploadTask uploadTask = null;
            uploadTask = storageReference.putBytes(mBytes);


            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //Now insert the download url into the firebase database
                    Uri firebaseURL = taskSnapshot.getDownloadUrl();

                    showToast("Cloud Backup Success");
                    //showToast( "Cloud Backup Success");
                    Log.d(TAG, "onSuccess: firebase download url : " + firebaseURL.toString());
                    hideDialog();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    showToast("Cloud Backup Failed");
                    //showToast( "Cloud Backup Failed");

                    hideDialog();

                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double currentProgress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    if(currentProgress > (progress + 15)){
                        progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        Log.d(TAG, "onProgress: Cloud Backup is " + progress + "% done");
                        showToast(progress + "%");
//                        Toast toast;
//                        showToast( progress + "%");
                    }

                }
            })
            ;
        }else{
            showToast("Image is too large");
            //showToast( "Image is too Large");
        }

    }



    /*
            ----------------------------- Firebase setup ---------------------------------
         */
    private void setupFirebaseAuth(){
        Log.d(TAG, "setupFirebaseAuth: started.");

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    //toastMessage("Successfully signed in with: " + user.getEmail());


                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                    showToast("Signed Out");
//                    showToast( "Signed out");
                    LoginFragment fragment = new LoginFragment();
                    android.support.v4.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    // replace whatever is in the fragment_container view with this fragment,
                    // and add the transaction to the back stack so the user can navigate back
                    transaction.replace(R.id.fragment_container, fragment);
                    transaction.addToBackStack(null);
                    transaction.commit();
                }
            }
        };
    }


    private void showDialog(){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    private void hideDialog(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

}
