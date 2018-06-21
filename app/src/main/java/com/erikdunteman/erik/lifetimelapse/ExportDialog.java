package com.erikdunteman.erik.lifetimelapse;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
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
import com.erikdunteman.erik.lifetimelapse.utils.ContextGetter;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class ExportDialog extends DialogFragment {

    private static final String TAG = "ExportDialog";


    private Project mProject;

    private ArrayList<String> mProjectNames;
    private EditText videoNameInput;
    private EditText videoLengthInput;


    //widgets
    private EditText mEmail;

    //vars
    private Context mContext;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_exportproject, container, false);


        videoNameInput = view.findViewById(R.id.exportName);
        videoLengthInput = view.findViewById(R.id.exportLength);

        mProject = getArguments().getParcelable("Project");
        final String timestamp = mProject.getProjPhotoTag();
        Log.d(TAG, "onCreateView: timestamp: " + timestamp);

        FFmpeg ffmpeg = FFmpeg.getInstance(getContext());
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {}

                @Override
                public void onFailure() {}

                @Override
                public void onSuccess() {
                    Log.d(TAG, "onSuccess: FFMPEG load successful");
                }

                @Override
                public void onFinish() {}
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
        }

        final TextView exportDialog = view.findViewById(R.id.dialogConfirm);
        exportDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: export commit clicked");

                String videoName = videoNameInput.getText().toString();
                if (videoName != null) {
                    double exportLength = Double.parseDouble(videoLengthInput.getText().toString());
                    SequencePhotos(videoName, timestamp, exportLength);
                }else{
                    Toast.makeText(mContext, "Please Enter A Video Duration.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    private void SequencePhotos(final String videoName, String timestamp, double exportLength) {

        //Determine if the photos pulled are supposed to have "Selfie" in the pathname
        String pathEnd;
        File folder = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() +  "/LifeTimeLapse/" + timestamp);
        Log.d(TAG, "SequencePhotos: folder name to string: " + folder.toString());
        //Get all images in the "folder" directory into an ArrayList of strings
        ArrayList<String> folderContentNames = new ArrayList<String>(Arrays.asList(folder.list()));
        Log.d(TAG, "SequencePhotos: folder contents to string: " + folderContentNames);
        if (folderContentNames.toString().contains("Selfie")){
            pathEnd = "Selfiepic.jpg";
        }else{
            pathEnd="pic.jpg";
        }

        int photoQuantity = folderContentNames.size();
        if (photoQuantity<=1){
            Toast.makeText(getContext(), "Export Failed: Project Too Short." , Toast.LENGTH_SHORT).show();
            dismiss();
        }

        double frameRate = photoQuantity/exportLength;
        String frameRateString = String.valueOf(frameRate);

        //A string for the image path by iterating the %d value
        String imagePath =Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() +  "/LifeTimeLapse/" + timestamp  + "/%04d" + pathEnd;


        String[] cmd = new String[]{
                "-y", "-framerate", frameRateString,
                "-i", imagePath,
                "-c:v", "libx264",
                "-vf","scale=640:-2",
                "-pix_fmt", "yuv420p",
                "-color_range", "0",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + videoName + ".mp4"};

        FFmpeg ffmpeg = FFmpeg.getInstance(getContext());
        try {
            Log.d(TAG, "SequencePhotos: Begining FFMPEG execute");
            Log.d(TAG, "SequencePhotos: cmd:   " + Arrays.toString(cmd));
            // to execute "ffmpeg -version" command you just need to pass "-version"
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                    Log.d(TAG, "onStart: Export started");
                    Toast.makeText(getContext(), "Export Started. \nPlease Wait.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onProgress(String message) {
                    Log.d(TAG, "onProgress: Export in progress");
                    Log.d(TAG, "onProgress: message: " + message);
                }

                @Override
                public void onFailure(String message) {
                    Log.d(TAG, "onFailure: Export Failed");
                    Log.d(TAG, "onFailure: message: " + message);
                    Toast.makeText(getContext(), "Export Failed", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, "onSuccess: Export Successful");
                    Toast.makeText(getContext(), "Export Complete!", Toast.LENGTH_LONG).show();
                    MediaScannerConnection.scanFile(ContextGetter.getAppContext(),
                            new String[] {Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + videoName + ".mp4"} ,
                            null, null);
                            Toast.makeText(getContext(), "Find " + videoName + " \nIn Your Media Gallery.", Toast.LENGTH_LONG).show();
                    dismiss();
                }

                @Override
                public void onFinish() {}
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            Log.d(TAG, "SequencePhotos: Error "  + e);
            // Handle if FFmpeg is already running
        }

}
}


















