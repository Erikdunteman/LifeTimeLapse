package com.erikdunteman.erik.lifetimelapse.models;

/**
 * Created by Erik on 1/14/2018.
 */

public class ProjectDB {

    //Declare Variables
    private String projname;
    private String projfreq;
    private String projlength;
    private String projlengthgoal;
    private String projphototag;

    //Project Constructor
    public ProjectDB(String projname, String projfreq, String projlength, String projlengthgoal, String projphototag) {
        this.projname = projname;
        this.projfreq = projfreq;
        this.projlength = projlength;
        this.projlengthgoal = projlengthgoal;
        this.projphototag = projphototag;
    }
    //Project Constructor
    public ProjectDB(){

    }

    //Getter and Setter
    public String getProjName() {
        return projname;
    }
    public void setProjName(String projname) {
        this.projname = projname;
    }
    public String getProjFreq() {
        return projfreq;
    }
    public void setProjFreq(String projfreq) {
        this.projfreq = projfreq;
    }
    public String getProjLength() {
        return projlength;
    }
    public void setProjLength(String projlength) {
        this.projlength = projlength;
    }
    public String getProjLengthGoal() {
        return projlengthgoal;
    }
    public void setProjLengthGoal(String projlengthgoal) {
        this.projlengthgoal = projlengthgoal;
    }
    public String getProjPhotoTag() {
        return projphototag;
    }
    public void setProjPhotoTag(String projphototag) {
        this.projphototag = projphototag;
    }

//String Full Project Meta
    @Override
    public String toString() {
        return "Projects{" +
                "projname='" + projname + '\'' +
                ", projfreq='" + projfreq + '\'' +
                ", projlength='" + projlength + '\'' +
                ", projlengthgoal='" + projlengthgoal + '\'' +
                ", projphototag='" + projphototag + '\'' +
                '}';
    }

}
