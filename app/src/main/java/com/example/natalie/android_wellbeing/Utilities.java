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

    static boolean validTime(Context context, int id){
        SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(context);
        List<String> times = dbHandler.getTimes(id);
        List<Integer> days = dbHandler.getDays(id);
        int timeCt = times.size();
        Boolean isValid = false;

        int currDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        if(days.contains(currDay - 1)){
            Log.i("VALID>>", "Valid day");
            for(int i = 0; i < timeCt; i++) {
                // Calculate survey start time
                int hr0 = Integer.parseInt(times.get(i).split(":")[0]);
                int min0 = Integer.parseInt(times.get(i).split(":")[1]);

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

    /*

    public void importSurveys(){
        final SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());
        final ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("SurveySummary");

        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> all_surveys, ParseException e) {
                if (e == null) {
                    for (int i = 0; i < all_surveys.size(); i++) {
                        final int survey_id = i;
                        ParseObject survey_listing = all_surveys.get(i);
                        if (survey_listing.getBoolean("Active") == true) {
                            final String name = survey_listing.getString("SurveyName");
                            final String time = survey_listing.getString("SurveyTime");
                            final int hr = Integer.parseInt(time.split(":")[0]);
                            final int min = Integer.parseInt(time.split(":")[1]);
                            final int duration = survey_listing.getInt("SurveyDuration");
                            final String table_name = survey_listing.getString("SurveyTableName");

                            // Get list of questions and their answers
                            final ParseQuery<ParseObject> query = new ParseQuery<ParseObject>(table_name);
                            query.findInBackground(new FindCallback<ParseObject>() {
                                public void done(List<ParseObject> survey, ParseException e) {
                                    if (e == null) {
                                        int ques_ct = survey.size();
                                        List<Object> ques = new ArrayList<Object>();
                                        List<Object> ans = new ArrayList<Object>();
                                        List<Object> type = new ArrayList<Object>();

                                        for (int j = 0; j < ques_ct; j++) {
                                            ParseObject curr_ques = survey.get(j);
                                            type.add(curr_ques.getString("QuestionType"));
                                            ques.add(curr_ques.getString("Question"));
                                            ans.add(j, join(curr_ques.getList("AnswerArray"), "\\|\\|"));
                                        }

                                        String ques_str = join(ques, "\\|\\|");
                                        String type_str = join(type, "\\|\\|");
                                        String ans_str = join(ans, "\\|nxt\\|");

                                        ParseUtil util = new ParseUtil();
                                        util.start_DialogAlarm(hr, min, survey_id);

                                        dbHandler.createSurvey(
                                                hr,
                                                min,
                                                duration,
                                                name,
                                                ques_str,
                                                ans_str,
                                                type_str
                                        );
                                    }
                                }
                            });
                        }
                    }
                }
            }
        });
    }

    public void start_DialogAlarm(int hr, int min, int table_id) {

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Alarm will trigger the pop-up dialog
        PendingIntent pendingIntent = PendingIntent.getService(
                getApplicationContext(),
                table_id,
                new Intent(getApplicationContext(), PopupService.class),
                PendingIntent.FLAG_CANCEL_CURRENT);


        // Set time for survey pop-up
        Calendar cal = Calendar.getInstance();

        // Get alarm time
        int curr_hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int curr_min = Calendar.getInstance().get(Calendar.MINUTE);

        // If it's after the alarm time, schedule for next day
        Toast.makeText(getApplicationContext(), curr_hr + ":" + curr_min + " " + hr + ":" + min, Toast.LENGTH_SHORT).show();
        if ( curr_hr > hr || curr_hr == hr && curr_min > min) {
            cal.add(Calendar.DAY_OF_YEAR, 1); // add, not set!
        }

        cal.set(Calendar.HOUR_OF_DAY, hr);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, 0);

        // Set alarm for survey pop-up to go off at default of 8:00 AM
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.getTimeInMillis(),
                alarmManager.INTERVAL_DAY,
                pendingIntent);
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
    */
}

