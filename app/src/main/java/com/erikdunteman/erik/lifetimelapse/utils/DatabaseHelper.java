package com.erikdunteman.erik.lifetimelapse.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.erikdunteman.erik.lifetimelapse.models.Project;

/**
 * Created by Erik on 1/23/2018.
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    private static final String DATABASE_NAME = "projects.db";
    private static final String TABLE_NAME = "projects_table";
    public static final String COL0 = "ID";
    public static final String COL1 = "NAME";
    public static final String COL2 = "PROJ_FREQ";
    public static final String COL3 = "PROJ_LENGTH";
    public static final String COL4 = "PROJ_LENGTH_GOAL";
    public static final String COL5 = "PROJ_PHOTO_TAG";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " +
                TABLE_NAME + " ( " +
                COL0 + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL1 + " TEXT, " +
                COL2 + " TEXT, " +
                COL3 + " TEXT, " +
                COL4 + " TEXT, " +
                COL5 + " TEXT )";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP IF TABLE EXISTS " + TABLE_NAME);
        onCreate(db);
    }


    //Insert new contact into database
    public boolean addProject(Project project){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL1, project.getProjName());
        contentValues.put(COL2, project.getProjFreq());
        contentValues.put(COL3, project.getProjLength());
        contentValues.put(COL4, project.getProjLengthGoal());
        contentValues.put(COL5, project.getProjPhotoTag());

        long result = db.insert(TABLE_NAME, null, contentValues);
        if(result == -1){
            return false;
        } else {
            return true;
        }
    }

    //Retrieve all contacts from database
    public Cursor getAllProjects(){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
    }

    //update contact where id = @param 'id'
    //Replace the current contact with @param 'contact'
    public boolean updateProject(Project project, int id){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL1, project.getProjName());
        contentValues.put(COL2, project.getProjFreq());
        contentValues.put(COL3, project.getProjLength());
        contentValues.put(COL4, project.getProjLengthGoal());
        contentValues.put(COL5, project.getProjPhotoTag());

        int update = db.update(TABLE_NAME, contentValues, COL0 + " = ? ", new String[] {String.valueOf(id)});

        //checking to see if we accidentally updated more than one row
        if(update != 1){
            return false;
        }else{
            return true;
        }
    };

    //Retrieve the contact unique id from the database using @param
    public Cursor getProjectID(Project project){
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "SELECT * FROM " +TABLE_NAME +
                " WHERE " + COL1 + " = '" + project.getProjName() +
                "' AND " + COL2 + " = '" + project.getProjPhotoTag()+"'";
        return db.rawQuery(sql,null);
    }

    public Integer deleteProject(int id){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, "ID = ?", new String[]{String.valueOf(id)});
    }
}
