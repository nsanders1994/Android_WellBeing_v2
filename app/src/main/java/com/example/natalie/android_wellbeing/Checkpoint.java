package com.example.natalie.android_wellbeing;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Natalie on 4/14/2015.
**/

public class Checkpoint extends Service {
    /**
     *  After the user clicks a notification or a user dialog in order to access a survey, Checkpoint
     *  is called to make sure the survey is still valid
    **/

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int id = intent.getIntExtra("ID", 1);   // requested survey's ID
        SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());

        // Get specified survey's active times, the active time duration, and its name
        List<String> times = dbHandler.getTimes(id);
        int duration       = dbHandler.getDuration(id);
        int timeCt = times.size();

        // Specifies whether the current time is valid, ie it is currently an active time
        boolean valid = false;

        // Check if current time is during any of the survey's active times
        for(int i = 0; i < timeCt; i++) {
            int hr = Integer.parseInt(times.get(i).split(":")[0]);  // active time: hour of day
            int min = Integer.parseInt(times.get(i).split(":")[1]); // active time: minute

            // Start of active time
            Calendar calInit0 = Calendar.getInstance();
            calInit0.set(Calendar.HOUR_OF_DAY, 0);
            calInit0.set(Calendar.MINUTE, 0);
            calInit0.set(Calendar.SECOND, 0);

            Calendar cal0 = calInit0;
            cal0.add(Calendar.HOUR_OF_DAY, hr);
            cal0.add(Calendar.MINUTE, min);

            // End of active time
            Calendar calInit1 = Calendar.getInstance();
            calInit1.set(Calendar.HOUR_OF_DAY, 0);
            calInit1.set(Calendar.MINUTE, 0);
            calInit1.set(Calendar.SECOND, 0);

            Calendar cal1 = calInit1;
            cal1.add(Calendar.HOUR_OF_DAY, hr);
            cal1.add(Calendar.MINUTE, min + duration);

            // Current time
            Calendar curr_cal = Calendar.getInstance();

            // If the current time falls between the start and end of the active time, the time is valid
            if(curr_cal.getTimeInMillis() >= cal0.getTimeInMillis() &&
               curr_cal.getTimeInMillis() <= cal1.getTimeInMillis()) {
                valid = true;
            }
        }

        // Specifies whether the survey has already been completed for the most recent active time
        boolean completed = dbHandler.isCompleted(id);

        // If it is currently an active time and the survey has not been completed, take the user to
        // the requested survey
        if(valid && !completed) {
            Intent surveyIntent = new Intent(Checkpoint.this, SurveyScreen.class);
            surveyIntent.putExtra("ID", id);
            surveyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(surveyIntent);
        }
        // Otherwise, take the user to the start screen and set the TOAST flag so that the get a
        // TOAST notification informing them that the survey is inactive
        else {
            Intent startIntent = new Intent(Checkpoint.this, StartScreen.class);
            startIntent.putExtra("TOAST", true);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
