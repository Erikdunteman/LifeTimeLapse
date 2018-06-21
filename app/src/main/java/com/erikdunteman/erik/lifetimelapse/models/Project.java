package com.erikdunteman.erik.lifetimelapse.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Erik on 1/14/2018.
 */

public class Project implements Parcelable {

    //Declare Variables
    private String projName;
    private String projFreq;
    private String projLength;
    private String projLengthGoal;
    private String projPhotoTag;

    //Project Constructor
    public Project(String projName, String projFreq, String projLength, String projLengthGoal, String projPhotoTag) {
        this.projName = projName;
        this.projFreq = projFreq;
        this.projLength = projLength;
        this.projLengthGoal = projLengthGoal;
        this.projPhotoTag = projPhotoTag;
    }

    //Getter and Setter
    public String getProjName() {
        return projName;
    }
    public void setProjName(String projName) {
        this.projName = projName;
    }
    public String getProjFreq() {
        return projFreq;
    }
    public void setProjFreq(String projFreq) {
        this.projFreq = projFreq;
    }
    public String getProjLength() {
        return projLength;
    }
    public void setProjLength(String projLength) {
        this.projLength = projLength;
    }
    public String getProjLengthGoal() {
        return projLengthGoal;
    }
    public void setProjLengthGoal(String projLengthGoal) {
        this.projLengthGoal = projLengthGoal;
    }
    public String getProjPhotoTag() {
        return projPhotoTag;
    }
    public void setProjPhotoTag(String projPhotoTag) {
        this.projPhotoTag = projPhotoTag;
    }

//String Full Project Meta
    @Override
    public String toString() {
        return "Projects{" +
                "projName='" + projName + '\'' +
                ", projFreq='" + projFreq + '\'' +
                ", projLength='" + projLength + '\'' +
                ", projLengthGoal='" + projLengthGoal + '\'' +
                ", projPhotoTag='" + projPhotoTag + '\'' +
                '}';
    }


    //Parcel Actions
    protected Project(Parcel in) {
        projName = in.readString();
        projFreq = in.readString();
        projLength = in.readString();
        projLengthGoal = in.readString();
        projPhotoTag = in.readString();
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(projName);
        dest.writeString(projFreq);
        dest.writeString(projLength);
        dest.writeString(projLengthGoal);
        dest.writeString(projPhotoTag);
    }
    @Override
    public int describeContents() {
        return 0;
    }
    public static final Creator<Project> CREATOR = new Creator<Project>() {
        @Override
        public Project createFromParcel(Parcel in) {
            return new Project(in);
        }
        @Override
        public Project[] newArray(int size) {
            return new Project[size];
        }
    };




}
