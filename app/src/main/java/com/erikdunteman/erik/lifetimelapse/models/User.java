package com.erikdunteman.erik.lifetimelapse.models;


import java.util.ArrayList;
import java.util.Arrays;


public class User {

    private String name;
    private String user_id;
    private String project_names;

    public User(String name, String user_id, String project_names) {
        this.name = name;
        this.user_id = user_id;
        this.project_names = project_names;
    }

    public User() {

    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", user_id='" + user_id + '\'' +
                ", project_names='" + project_names + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getProjectNames() {
        if(project_names!=null) {
            ArrayList<String> array = new ArrayList(Arrays.asList(project_names.split(", ")));
            return array;
        } else {
            return null;
        }
    }

    public void setProjectNames(ArrayList<String> project_names_array) {


        StringBuilder project_names = new StringBuilder();
        for (String s : project_names_array)
        {
            project_names.append(s);
            project_names.append(", ");
        }

        this.project_names = project_names.toString();
    }

}
