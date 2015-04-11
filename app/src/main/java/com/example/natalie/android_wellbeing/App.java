package com.example.natalie.android_wellbeing;

import android.app.Application;
import android.os.Bundle;

import com.parse.Parse;

/**
 * Created by Natalie on 4/9/2015.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Parse
        Parse.enableLocalDatastore(this);
        Parse.initialize(this,
                "Z6S6iux9qyLGcCsAE3vuRvhHWDwFelxzT2nSqKWc",
                "boXMTOaotk2HgGpxFLdNNPFw1d7WwB7c3G4nPHak");

    }
}
