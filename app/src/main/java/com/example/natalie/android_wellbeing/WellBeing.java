package com.example.natalie.android_wellbeing;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Natalie on 4/9/2015.
 */
public class WellBeing extends Application {
    List<String> ID_list = new ArrayList<>();
    static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();

        // Set Context
        appContext = getApplicationContext();

        // Initialize Parse
        Parse.enableLocalDatastore(this);

        Parse.initialize(this, "wFcqaTXYYCeNqKJ8wswlwtXChEzJyFyBV7N5JOZX", "MomzqWhPQSVPNZ6hNjXtSSs6Lah5OMQCE8p4amsW");
                /*"Z6S6iux9qyLGcCsAE3vuRvhHWDwFelxzT2nSqKWc",
                "boXMTOaotk2HgGpxFLdNNPFw1d7WwB7c3G4nPHak");*/

        final SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());


        // If app was just installed get all surveys from Parse and store in database
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean previouslyStarted = prefs.getBoolean(getString(R.string.prev_started), false);

        if(!previouslyStarted) {
            // Query Survey Table
            final ParseQuery<ParseObject> query = new ParseQuery<>("SurveySummary");
            query.whereEqualTo("Active", true);
            query.findInBackground(new FindCallback<ParseObject>() {
                public void done(List<ParseObject> all_surveys, ParseException e) {
                if (e == null) {
                    int surveyCt = all_surveys.size();

                    // For each survey listed in the table
                    for (int i = 0; i < surveyCt; i++) {

                        ParseObject survey_listing = all_surveys.get(i);
                        final String name          = survey_listing.getString("Category");          // Type of survey
                        final List<Object> times   = survey_listing.getList("Time");                // Active times of survey
                        final int duration         = survey_listing.getInt("surveyActiveDuration"); // Duration of active time
                        final String table_name    = survey_listing.getString("Survey");            // Survey name
                        final int surveyVersion    = survey_listing.getInt("Version");              // Survey version
                        final List<Object> days    = survey_listing.getList("Days");                // Days survey has active times

                        Log.i("DEBUG>>>", "Name = " + name);
                        // Get list of questions and their answers
                        ParseQuery<ParseObject> query2 = new ParseQuery<ParseObject>(table_name);
                        query2.orderByAscending("questionId");
                        query2.findInBackground(new FindCallback<ParseObject>() {
                            public void done(List<ParseObject> survey, ParseException e) {
                                if (e == null) {
                                    int ques_ct = survey.size();
                                    List<Object> ques = new ArrayList<>(ques_ct);       // Question string
                                    List<Object> ans = new ArrayList<>(ques_ct);        // Possible answer stings
                                    List<Object> type = new ArrayList<>(ques_ct);       // Type of question
                                    List<Object> ansVals = new ArrayList<>(ques_ct);    // Chosen values for answers
                                    List<Object> endpts = new ArrayList<>(ques_ct);     // Endpoints of the scale for slider questions

                                    // For each question in the current survey
                                    for (int j = 0; j < ques_ct; j++) {
                                        ParseObject curr_ques = survey.get(j);

                                        type.add(curr_ques.getString("questionType"));
                                        ques.add(curr_ques.getString("question"));

                                        // If the question is a textbox question, the answer info is not needed
                                        if(curr_ques.getString("questionType").equals("Textbox")){
                                            ans.add("NA");
                                            ansVals.add(-1);
                                        }
                                        else{
                                            ans.add(Utilities.join(curr_ques.getList("options"), "%%"));
                                            ansVals.add(Utilities.join(curr_ques.getList("numericScale"), "%%"));
                                        }

                                        List<Object> temp = curr_ques.getList("endPoints");
                                        if(temp.size() != 0){
                                            endpts.add(Utilities.join(temp, "%%"));
                                        }else{
                                            endpts.add("-%%-");
                                        }

                                    }

                                    // Join lists by delimiter in preparation to store as strings
                                    String ques_str = Utilities.join(ques, "%%");
                                    String type_str = Utilities.join(type, "%%");
                                    String ans_str = Utilities.join(ans, "%nxt%");
                                    String ansVal_str = Utilities.join(ansVals, "%nxt%");
                                    String endPts_str = Utilities.join(endpts, "%nxt%");

                                    // Store new survey in SQLite database
                                    dbHandler.createSurvey(
                                            Utilities.join(times, ","),
                                            duration,
                                            name,
                                            ques_str,
                                            ans_str,
                                            type_str,
                                            ansVal_str,
                                            surveyVersion,
                                            Utilities.join(days, ","),
                                            endPts_str
                                    );

                                    // Set alarms for the survey notifications
                                    int survey_id = dbHandler.getLastRowID();
                                    int iteration = 1;
                                    Log.i("TIME>>>", "name = " + name + ", ID = " + String.valueOf(survey_id));

                                    int dayCt = days.size();
                                    int timeCt = times.size();
                                    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                                    // For every active day...
                                    for(int d = 0; d < dayCt; d++) {
                                        int currDay = Integer.parseInt(String.valueOf(days.get(d))) + 1; // days are given 0-6, android uses 1-7

                                        // ...set an alarm for all survey times
                                        for(int j = 0; j < timeCt; j++) {
                                            int hr  = Integer.parseInt(String.valueOf(times.get(j)).split(":")[0]);
                                            int min = Integer.parseInt(String.valueOf(times.get(j)).split(":")[1]);

                                            // Get current time
                                            Calendar curr_cal = Calendar.getInstance();

                                            // Get alarm time
                                            Calendar cal = Calendar.getInstance();
                                            cal.set(Calendar.DAY_OF_WEEK, currDay);
                                            cal.set(Calendar.HOUR_OF_DAY, hr);
                                            cal.set(Calendar.MINUTE, min);
                                            cal.set(Calendar.SECOND, 0);

                                            String timeStr = String.valueOf(cal.get(Calendar.HOUR_OF_DAY)) + ":" + String.valueOf(cal.get(Calendar.MINUTE));

                                            // If it's after the alarm time, schedule alarm for next week
                                            if ( curr_cal.getTimeInMillis() >= cal.getTimeInMillis()) {
                                                Log.i("TIME>>>", "\tSurvey alarm scheduled for next week");
                                                cal.add(Calendar.DAY_OF_YEAR, 7); // add, not set!

                                                // Check if any of the following alarms for this active period are not past
                                                for(int k = 1; k < 4; k++) {
                                                    // Get alarm time
                                                    Calendar cal2 = Calendar.getInstance();
                                                    cal2.set(Calendar.DAY_OF_WEEK, currDay);
                                                    cal2.set(Calendar.HOUR_OF_DAY, hr);
                                                    cal2.set(Calendar.MINUTE, min + k*(duration/4));
                                                    cal2.set(Calendar.SECOND, 0);

                                                    // If there is an iteration that isn't past the current time, schedule alarm
                                                    if ( curr_cal.getTimeInMillis() < cal2.getTimeInMillis()) {
                                                        int iter = k + 1;
                                                        String partID = String.valueOf(survey_id) + // survey id
                                                                        String.valueOf(d) +         // day
                                                                        String.valueOf(j);          // time

                                                        int intentID = Integer.parseInt(partID + String.valueOf(iter));

                                                        Intent intent;
                                                        intent = new Intent(getApplicationContext(), NotificationService.class);
                                                        intent.putExtra("ID", survey_id);
                                                        intent.putExtra("PART_ID", partID);
                                                        intent.putExtra("ITER", iter);
                                                        intent.putExtra("TIME", String.valueOf(hr) + ":" + String.valueOf(min));

                                                        PendingIntent notifPendingIntent2 = PendingIntent.getService(
                                                                getApplicationContext(),
                                                                intentID,
                                                                intent,
                                                                PendingIntent.FLAG_CANCEL_CURRENT);

                                                        Log.i("TIME>>>", "Secondary Alarm for " + String.valueOf(survey_id) + " = " + cal2.getTime().toString());
                                                        // Set alarm for survey notification
                                                        alarmManager.set(AlarmManager.RTC_WAKEUP, cal2.getTimeInMillis(), notifPendingIntent2);

                                                        break;
                                                    }
                                                }
                                            }

                                            Log.i("TIME>>>", "Primary Alarm for " + String.valueOf(survey_id) + " = " + cal.getTime().toString());

                                            String partID = String.valueOf(d) + String.valueOf(survey_id) + String.valueOf(j);
                                            int intentID  = Integer.parseInt(partID + "1");

                                            Intent notifIntent = new Intent(getApplicationContext(), NotificationService.class);
                                            notifIntent.putExtra("ID", survey_id);
                                            notifIntent.putExtra("PART_ID", partID);
                                            notifIntent.putExtra("ITER", 1);
                                            notifIntent.putExtra("TIME", String.valueOf(hr) + ":" + String.valueOf(min));

                                            PendingIntent notifPendingIntent = PendingIntent.getService(
                                                    getApplicationContext(),
                                                    intentID,
                                                    notifIntent,
                                                    PendingIntent.FLAG_CANCEL_CURRENT);

                                            // Set alarm for survey notification
                                            alarmManager.set/*Repeating*/(
                                                    AlarmManager.RTC_WAKEUP,
                                                    cal.getTimeInMillis(),
                                                    //7*alarmManager.INTERVAL_DAY,
                                                    notifPendingIntent);
                                        }
                                    }
                                }
                            }
                        });
                    }

                    SharedPreferences.Editor edit = prefs.edit();
                    edit.putBoolean(getString(R.string.prev_started), Boolean.TRUE);
                    edit.commit();
                }
                else {
                    Log.i("DEBUG>>>", "Parse initial survey request failed");
                }
                }
            });
        }
    }

    static public Context getWellbeingContext(){
        return appContext;
    }
}
