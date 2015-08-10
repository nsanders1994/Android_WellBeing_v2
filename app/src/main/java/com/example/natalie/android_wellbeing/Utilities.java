package com.example.natalie.android_wellbeing;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Natalie on 3/23/2015.
 */

public class Utilities extends Activity {
    private boolean done = false;

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    public static String join(List<Object> list, String delimiter) {
        int ct = list.size();
        String str = "";
        for(int i = 0; i < ct; i++) {
            str = str + String.valueOf(list.get(i));
            if(i + 1 != ct) {
                str = str + delimiter;
            }
        }

        return str;
    }

    static boolean surveyOpen(Context context, int id){
        SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(context);
        List<String> times = dbHandler.getTimes(id);
        List<Integer> days = dbHandler.getDays(id);
        int timeCt = times.size();
        Boolean isValid = false;

        int currDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);

        if (days.get(0) == -1) {
            isValid = true;
        }
        else if(days.contains(currDay - 1)){
            Log.i("VALID>>", "Valid day");
            for(int i = 0; i < timeCt; i++) {
                // Calculate survey start time
                int hr0 = Integer.parseInt(times.get(i).split(":")[0]);
                int min0 = Integer.parseInt(times.get(i).split(":")[1]);

                Log.i("VALID>>>", "Time = " + String.valueOf(hr0) + ":" + String.valueOf(min0));

                Calendar calendar0 = Calendar.getInstance();
                calendar0.set(Calendar.HOUR_OF_DAY, hr0);
                calendar0.set(Calendar.MINUTE, min0);
                calendar0.set(Calendar.SECOND, 0);

                // Calculate survey closing time
                int duration = dbHandler.getDuration(id);

                Calendar calendar1 = Calendar.getInstance();
                calendar1.set(Calendar.HOUR_OF_DAY, hr0);
                calendar1.set(Calendar.MINUTE, min0 + duration);
                calendar0.set(Calendar.SECOND, 0);

                // Set clickable/unclickable
                boolean completed = dbHandler.isCompleted(id);
                Calendar curr_calendar = Calendar.getInstance();

                if (!completed &&
                        curr_calendar.getTimeInMillis() >= calendar0.getTimeInMillis() &&
                        curr_calendar.getTimeInMillis() <= calendar1.getTimeInMillis()) {
                    Log.i("VALID>>", "Is valid");
                    isValid = true;
                }

                Log.i("VALID>>", "curr time   = " + String.valueOf(curr_calendar.getTimeInMillis()));
                Log.i("VALID>>", "alarm start = " + String.valueOf(calendar0.getTimeInMillis()));
                Log.i("VALID>>", "alarm end   = " + String.valueOf(calendar1.getTimeInMillis()));

            }
        }

        return isValid;
    }

    static boolean timeValid(Context context, int id){
        SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(context);
        List<String> times = dbHandler.getTimes(id);
        List<Integer> days = dbHandler.getDays(id);
        int timeCt = times.size();
        Boolean isValid = false;

        int currDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);

        if (days.get(0) == -1) {
            isValid = true;
        }
        else if(days.contains(currDay - 1)){
            Log.i("VALID>>", "Valid day");
            for(int i = 0; i < timeCt; i++) {
                // Calculate survey start time
                int hr0 = Integer.parseInt(times.get(i).split(":")[0]);
                int min0 = Integer.parseInt(times.get(i).split(":")[1]);

                Log.i("VALID>>>", "Time = " + String.valueOf(hr0) + ":" + String.valueOf(min0));

                Calendar calendar0 = Calendar.getInstance();
                calendar0.set(Calendar.HOUR_OF_DAY, hr0);
                calendar0.set(Calendar.MINUTE, min0);
                calendar0.set(Calendar.SECOND, 0);

                // Calculate survey closing time
                int duration = dbHandler.getDuration(id);

                Calendar calendar1 = Calendar.getInstance();
                calendar1.set(Calendar.HOUR_OF_DAY, hr0);
                calendar1.set(Calendar.MINUTE, min0 + duration);
                calendar0.set(Calendar.SECOND, 0);

                // Set clickable/unclickable
                boolean completed = dbHandler.isCompleted(id);
                Calendar curr_calendar = Calendar.getInstance();

                if (curr_calendar.getTimeInMillis() >= calendar0.getTimeInMillis() &&
                    curr_calendar.getTimeInMillis() <= calendar1.getTimeInMillis()) {
                    Log.i("VALID>>", "Is valid");
                    isValid = true;
                }

                Log.i("VALID>>", "curr time   = " + String.valueOf(curr_calendar.getTimeInMillis()));
                Log.i("VALID>>", "alarm start = " + String.valueOf(calendar0.getTimeInMillis()));
                Log.i("VALID>>", "alarm end   = " + String.valueOf(calendar1.getTimeInMillis()));

            }
        }

        return isValid;
    }

    static public boolean is24HrSurvey(Context context, int id){
        SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(context);
        List<Integer> days = dbHandler.getDays(id);

        if(days.get(0) == -1){
            return true;
        }
        else {
            return false;
        }
    }
}

