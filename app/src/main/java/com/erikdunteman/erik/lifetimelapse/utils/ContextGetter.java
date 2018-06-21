package com.erikdunteman.erik.lifetimelapse.utils;

import android.app.Application;
import android.content.Context;

/**
 * Created by Erik on 3/11/2018.
 */

public class ContextGetter extends Application {
    private static Context context;

    public void onCreate() {
        super.onCreate();
        ContextGetter.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return ContextGetter.context;
    }

}
