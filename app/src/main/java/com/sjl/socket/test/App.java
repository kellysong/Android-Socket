
package com.sjl.socket.test;

import android.app.Application;

import androidx.annotation.NonNull;

public class App extends Application {

    private static App mInstance;


    @Override
    public void onCreate() {
        super.onCreate();

        if (mInstance == null) {
            mInstance = this;

        }
    }

    @NonNull
    public static App getInstance() {
        return mInstance;
    }



}