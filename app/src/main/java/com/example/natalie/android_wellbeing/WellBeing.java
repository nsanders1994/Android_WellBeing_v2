package com.example.natalie.android_wellbeing;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
                                        Log.i("DEBUG>>>", "name = " + name + ", ID = " + String.valueOf(survey_id));

                                        for(int j = 0; j < time.size(); j++) {
                                            int hr  = Integer.parseInt(String.valueOf(time.get(j)).split(":")[0]);
                                            int min = Integer.parseInt(String.valueOf(time.get(j)).split(":")[1]);

                                            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                                            int inc = 0;
                                            for(int i = 0; i < 4; i++){
                                                // Get curr time
                                                Calendar curr_cal = Calendar.getInstance();

                                                // Get alarm time
                                                Calendar cal = Calendar.getInstance();
                                                cal.set(Calendar.HOUR_OF_DAY, hr);
                                                cal.set(Calendar.MINUTE, min + inc);
                                                cal.set(Calendar.SECOND, 0);

                                                // If it's after the alarm time, schedule for next day
                                                if ( curr_cal.getTimeInMillis() > cal.getTimeInMillis()) {

                                                    Log.i("DEBUG>>>", "\tSurvey alarm scheduled for next day");
                                                    cal.add(Calendar.DAY_OF_YEAR, 1); // add, not set!
                                                }

                                                Log.i("DEBUG>>>", "\tAlarm set for " + cal.getTime().toString());
                                                if(i < 3) {
                                                    Intent notifIntent = new Intent(getApplicationContext(), NotificationService.class);
                                                    notifIntent.putExtra("ID", survey_id);

                                                    PendingIntent notifPendingIntent = PendingIntent.getService(
                                                            getApplicationContext(),
                                                            Integer.parseInt(String.valueOf(survey_id) + String.valueOf(j) + String.valueOf(i)),
                                                            notifIntent,
                                                            PendingIntent.FLAG_CANCEL_CURRENT);

                                                    // Set alarm for survey notification
                                                    alarmManager.setRepeating(
                                                            AlarmManager.RTC_WAKEUP,
                                                            cal.getTimeInMillis(),
                                                            alarmManager.INTERVAL_DAY,
                                                            notifPendingIntent);
                                                }
                                                else {
                                                    Intent dialogIntent = new Intent(getApplicationContext(), PopupService.class);
                                                    dialogIntent.putExtra("ID", survey_id);

                                                    PendingIntent dialogPendingIntent = PendingIntent.getService(
                                                            getApplicationContext(),
                                                            Integer.parseInt(String.valueOf(survey_id) + String.valueOf(j) + String.valueOf(i)),
                                                            dialogIntent,
                                                            PendingIntent.FLAG_CANCEL_CURRENT);

                                                    // Set alarm for survey diolog
                                                    alarmManager.setRepeating(
                                                            AlarmManager.RTC_WAKEUP,
                                                            cal.getTimeInMillis(),
                                                            alarmManager.INTERVAL_DAY,
                                                            dialogPendingIntent);
                                                }


                                                inc += duration/4;
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
}
