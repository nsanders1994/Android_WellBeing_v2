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
import java.util.List;

/**
 * Created by Natalie on 4/9/2015.
 */
public class WellBeing extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Parse
        Parse.enableLocalDatastore(this);

        Parse.initialize(this, /*"wFcqaTXYYCeNqKJ8wswlwtXChEzJyFyBV7N5JOZX", "MomzqWhPQSVPNZ6hNjXtSSs6Lah5OMQCE8p4amsW");*/
                "Z6S6iux9qyLGcCsAE3vuRvhHWDwFelxzT2nSqKWc",
                "boXMTOaotk2HgGpxFLdNNPFw1d7WwB7c3G4nPHak");

        final SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());

        // If app was just installed get all surveys from Parse and store in database
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean previouslyStarted = prefs.getBoolean(getString(R.string.prev_started), false);

        if(!previouslyStarted) {
            final ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("SurveySummary");
            query.whereEqualTo("Active", true);
            query.findInBackground(new FindCallback<ParseObject>() {
                public void done(List<ParseObject> all_surveys, ParseException e) {
                    if (e == null) {
                        int surveyCt = all_surveys.size();
                        for (int i = 0; i < surveyCt; i++) {

                            ParseObject survey_listing = all_surveys.get(i);
                            final String name          = survey_listing.getString("Category");
                            final List<Object> time    = survey_listing.getList("Time");
                            final int duration         = survey_listing.getInt("surveyActiveDuration");
                            final String table_name    = survey_listing.getString("Survey");
                            final int surveyVersion    = survey_listing.getInt("Version");

                            // Get list of questions and their answers
                            ParseQuery<ParseObject> query2 = new ParseQuery<ParseObject>(table_name);
                            query2.orderByAscending("questionId");
                            query2.findInBackground(new FindCallback<ParseObject>() {
                                public void done(List<ParseObject> survey, ParseException e) {
                                    if (e == null) {
                                        int ques_ct = survey.size();
                                        Log.i("DEBUG>>>>>", "ques_ct =" + String.valueOf(ques_ct));
                                        List<Object> ques = new ArrayList<>(ques_ct);
                                        List<Object> ans = new ArrayList<>(ques_ct);
                                        List<Object> type = new ArrayList<>(ques_ct);
                                        List<Object> ansVals = new ArrayList<>(ques_ct);

                                        for (int j = 0; j < ques_ct; j++) {
                                            ParseObject curr_ques = survey.get(j);
                                            //int qID = curr_ques.getInt("questionId") - 1;

                                            type.add(curr_ques.getString("questionType"));
                                            ques.add(curr_ques.getString("question"));
                                            ans.add(Utilities.join(curr_ques.getList("options"), "%%"));
                                            ansVals.add(Utilities.join(curr_ques.getList("numericScale"), "%%"));
                                        }

                                        String ques_str = Utilities.join(ques, "%%");
                                        String type_str = Utilities.join(type, "%%");
                                        String ans_str = Utilities.join(ans, "%nxt%");
                                        String ansVal_str = Utilities.join(ansVals, "%nxt%");

                                        dbHandler.createSurvey(
                                                Utilities.join(time, ","),
                                                duration,
                                                name,
                                                ques_str,
                                                ans_str,
                                                type_str,
                                                ansVal_str,
                                                surveyVersion
                                        );


                                        int survey_id = dbHandler.getLastRowID();
                                        int iteration = 1;
                                        Log.i("DEBUG>>>", "name = " + name + ", ID = " + String.valueOf(survey_id));

                                        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                                        // Set first alarm for all survey times
                                        for(int j = 0; j < time.size(); j++) {
                                            int hr  = Integer.parseInt(String.valueOf(time.get(j)).split(":")[0]);
                                            int min = Integer.parseInt(String.valueOf(time.get(j)).split(":")[1]);

                                            // Get curr time
                                            Calendar curr_cal = Calendar.getInstance();

                                            // Get alarm time
                                            Calendar cal = Calendar.getInstance();
                                            cal.set(Calendar.HOUR_OF_DAY, hr);
                                            cal.set(Calendar.MINUTE, min);
                                            cal.set(Calendar.SECOND, 0);

                                            String time = String.valueOf(cal.get(Calendar.HOUR_OF_DAY)) + ":" + String.valueOf(cal.get(Calendar.MINUTE));

                                            // If it's after the alarm time, schedule starting alarm for next day
                                            if ( curr_cal.getTimeInMillis() > cal.getTimeInMillis()) {
                                                Log.i("DEBUG>>>", "\tSurvey alarm scheduled for next day");
                                                cal.add(Calendar.DAY_OF_YEAR, 1); // add, not set!

                                                // Check if any of the following alarms for this active period are not past
                                                for(int k = 1; k < 4; k++) {
                                                    // Get alarm time
                                                    Calendar cal2 = Calendar.getInstance();
                                                    cal2.set(Calendar.HOUR_OF_DAY, hr);
                                                    cal2.set(Calendar.MINUTE, min + k*(duration/4));
                                                    cal2.set(Calendar.SECOND, 0);

                                                    if ( curr_cal.getTimeInMillis() < cal2.getTimeInMillis()) {
                                                        String time2 = String.valueOf(cal2.get(Calendar.HOUR_OF_DAY) + ":" +
                                                                       String.valueOf(cal2.get(Calendar.MINUTE)));
                                                        int iter = k + 1;

                                                        Intent intent;
                                                        if(iter != 4) {
                                                            intent = new Intent(getApplicationContext(), NotificationService.class);
                                                            intent.putExtra("SID", survey_id);
                                                            intent.putExtra("tID", j);
                                                            intent.putExtra("T_CURR", time2);
                                                            intent.putExtra("ITER", iter);
                                                        }
                                                        else {
                                                            intent = new Intent(getApplicationContext(), PopupService.class);
                                                            intent.putExtra("SID", survey_id);
                                                            intent.putExtra("TID", j);
                                                            intent.putExtra("T_CURR", time2);
                                                            intent.putExtra("ITER", iter);
                                                        }

                                                        PendingIntent notifPendingIntent2 = PendingIntent.getService(
                                                                getApplicationContext(),
                                                                Integer.parseInt(String.valueOf(survey_id) + String.valueOf(j) + String.valueOf(iter)), // alarm id code is <survey id> <survey time id> <iteration of survey time>
                                                                intent,
                                                                PendingIntent.FLAG_CANCEL_CURRENT);

                                                        Log.i("DEBUG>>>", "Secondary Alarm for " + String.valueOf(survey_id) + " = " + cal2.getTime().toString());
                                                        // Set alarm for survey notification
                                                        alarmManager.set(AlarmManager.RTC_WAKEUP, cal2.getTimeInMillis(), notifPendingIntent2);


                                                        break;
                                                    }
                                                }
                                            }

                                            Log.i("DEBUG>>>", "Primary Alarm for " + String.valueOf(survey_id) + " = " + cal.getTime().toString());

                                            Intent notifIntent = new Intent(getApplicationContext(), NotificationService.class);
                                            notifIntent.putExtra("SID", survey_id);
                                            notifIntent.putExtra("TID", j);
                                            notifIntent.putExtra("T_CURR", time);
                                            notifIntent.putExtra("ITER", iteration);

                                            PendingIntent notifPendingIntent = PendingIntent.getService(
                                                    getApplicationContext(),
                                                    Integer.parseInt(String.valueOf(survey_id) + String.valueOf(j) + String.valueOf(iteration)), // alarm id code is <survey id> <survey time id> <iteration of survey time>
                                                    notifIntent,
                                                    PendingIntent.FLAG_CANCEL_CURRENT);

                                            // Set alarm for survey notification
                                            alarmManager.setRepeating(
                                                    AlarmManager.RTC_WAKEUP,
                                                    cal.getTimeInMillis(),
                                                    alarmManager.INTERVAL_DAY,
                                                    notifPendingIntent);

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
}
