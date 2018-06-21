package com.erikdunteman.erik.lifetimelapse.utils;

/**
 * Created by Erik on 1/28/2018.
 */

import android.os.Handler;


public class Delay {

    // Delay mechanism

    public interface DelayCallback{
        void afterDelay();
    }

    public static void delay(int millisecs, final DelayCallback delayCallback){
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                delayCallback.afterDelay();
            }
        }, millisecs); // afterDelay will be executed after given milliseconds.
    }
}
