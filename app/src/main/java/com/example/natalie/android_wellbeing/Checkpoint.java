package com.example.natalie.android_wellbeing;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.security.Provider;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Natalie on 4/14/2015.
 */
public class Checkpoint extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int id = intent.getIntExtra("ID", 1);
        SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());

        List<String> times = dbHandler.getTimes(id);
        int duration       = dbHandler.getDuration(id);
        String survey_name = dbHandler.getName(id);

        Log.i("DEBUG>>>", "In Checkpoint, ID for " + survey_name + " = " + String.valueOf(id));
        int timeCt = times.size();
        boolean valid = false;

        for(int i = 0; i < timeCt; i++) {
            int hr = Integer.parseInt(times.get(i).split(":")[0]);
            int min = Integer.parseInt(times.get(i).split(":")[1]);

            Calendar calInit0 = Calendar.getInstance();
            calInit0.set(Calendar.HOUR_OF_DAY, 0);
            calInit0.set(Calendar.MINUTE, 0);
            calInit0.set(Calendar.SECOND, 0);

            Calendar cal0 = calInit0;
            cal0.add(Calendar.HOUR_OF_DAY, hr);
            cal0.add(Calendar.MINUTE, min);

            Calendar calInit1 = Calendar.getInstance();
            calInit1.set(Calendar.HOUR_OF_DAY, 0);
            calInit1.set(Calendar.MINUTE, 0);
            calInit1.set(Calendar.SECOND, 0);

            Calendar cal1 = calInit1;
            cal1.add(Calendar.HOUR_OF_DAY, hr);
            cal1.add(Calendar.MINUTE, min + duration);
            
            Calendar curr_cal = Calendar.getInstance();

            if(curr_cal.getTimeInMillis() >= cal0.getTimeInMillis() &&
               curr_cal.getTimeInMillis() <= cal1.getTimeInMillis()) {
                valid = true;
            }
        }

        Log.i("DEBUG>>>", "NAME = " + dbHandler.getName(id));

        if(valid) {
            Intent surveyIntent = new Intent(Checkpoint.this, SurveyScreen.class);
            surveyIntent.putExtra("ID", id);
            surveyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(surveyIntent);
        }
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
        //TODO for communication return IBinder implementation
        return null;
    }
}
