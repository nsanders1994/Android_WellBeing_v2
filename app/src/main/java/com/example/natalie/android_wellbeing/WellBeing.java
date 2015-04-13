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
        Parse.initialize(this,
                "Z6S6iux9qyLGcCsAE3vuRvhHWDwFelxzT2nSqKWc",
                "boXMTOaotk2HgGpxFLdNNPFw1d7WwB7c3G4nPHak");

        final SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());

        // If app was just installed get all surveys from Parse and store in database
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean previouslyStarted = prefs.getBoolean(getString(R.string.prev_started), false);
        if(!previouslyStarted) {

            final ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("SurveySummary");

            query.findInBackground(new FindCallback<ParseObject>() {
                public void done(List<ParseObject> all_surveys, ParseException e) {
                    if (e == null) {
                        for (int i = 0; i < all_surveys.size(); i++) {
                            final int survey_id = i;

                            ParseObject survey_listing = all_surveys.get(i);
                            boolean active = survey_listing.getBoolean("Active");

                            if(!active) continue; // If the current survey is not active skip to the next survey

                            final String name          = survey_listing.getString("Category");
                            final List<Object> time    = survey_listing.getList("Time");
                            final int duration         = survey_listing.getInt("surveyActiveDuration");
                            final String table_name    = survey_listing.getString("Survey");
                            final int surveyVersion    = survey_listing.getInt("Version");

                            // Get list of questions and their answers
                            ParseQuery<ParseObject> query2 = new ParseQuery<ParseObject>(table_name);
                            query2.findInBackground(new FindCallback<ParseObject>() {
                                public void done(List<ParseObject> survey, ParseException e) {
                                    if (e == null) {
                                        int ques_ct = survey.size();
                                        Log.i("DEBUG>>>>>", "ques_ct =" + String.valueOf(ques_ct));
                                        List<Object> ques = new ArrayList<>(ques_ct);
                                        List<Object> ans = new ArrayList<>(ques_ct);
                                        List<Object> type = new ArrayList<>(ques_ct);
                                        List<Object> ansVals = new ArrayList<>(ques_ct);

                                        for(int k = 0; k < ques_ct; k++) {
                                            type.add("filler");
                                            ques.add("filler");
                                            ans.add (new ArrayList<>());
                                            ansVals.add(new ArrayList<>());
                                        }
                                        Log.i("DEBUG>>>>>", "array size = " + String.valueOf(type.size()));


                                        for (int j = 0; j < ques_ct; j++) {
                                            ParseObject curr_ques = survey.get(j);
                                            int qID = curr_ques.getInt("questionId") - 1;

                                            type.set(qID, curr_ques.getString("questionType"));
                                            ques.set(qID, curr_ques.getString("question"));
                                            ans.set(qID, Utilities.join(curr_ques.getList("options"), "%%"));
                                            ansVals.set(qID, Utilities.join(curr_ques.getList("numericScale"), "%%"));
                                        }

                                        String ques_str = Utilities.join(ques, "%%");
                                        String type_str = Utilities.join(type, "%%");
                                        String ans_str = Utilities.join(ans, "%nxt%");
                                        String ansVal_str = Utilities.join(ansVals, "%nxt%");

                                        for(int j = 0; j < time.size(); j++) {
                                            int hr  = Integer.parseInt(String.valueOf(time.get(j)).split(":")[0]);
                                            int min = Integer.parseInt(String.valueOf(time.get(j)).split(":")[1]);

                                            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                                            Calendar cal = Calendar.getInstance();
                                            cal.set(Calendar.HOUR_OF_DAY, hr);
                                            cal.set(Calendar.MINUTE, min);
                                            cal.set(Calendar.SECOND, 0);

                                            // Create intents
                                            Intent dialogIntent = new Intent(getApplicationContext(), PopupService.class);
                                            dialogIntent.putExtra("ID", survey_id);
                                            dialogIntent.putExtra("ITERATION", j);

                                            Intent notifIntent = new Intent(getApplicationContext(), NotificationService.class);
                                            notifIntent.putExtra("ID", survey_id);
                                            notifIntent.putExtra("ITERATION", j);

                                            PendingIntent dialogPendingIntent = PendingIntent.getService(
                                                    getApplicationContext(),
                                                    Integer.parseInt(String.valueOf(survey_id) + String.valueOf(j)),
                                                    dialogIntent,
                                                    PendingIntent.FLAG_CANCEL_CURRENT);

                                            PendingIntent notifPendingIntent = PendingIntent.getService(
                                                    getApplicationContext(),
                                                    Integer.parseInt(String.valueOf(survey_id) + String.valueOf(j)),
                                                    notifIntent,
                                                    PendingIntent.FLAG_CANCEL_CURRENT);

                                            for(int i = 0; i < 4; i++){
                                                // Get alarm time
                                                int curr_hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                                                int curr_min = Calendar.getInstance().get(Calendar.MINUTE);

                                                // If it's after the alarm time, schedule for next day
                                                if ( curr_hr > hr || curr_hr == cal.get(Calendar.HOUR_OF_DAY)
                                                        && curr_min > cal.get(Calendar.MINUTE)) {
                                                    cal.add(Calendar.DAY_OF_YEAR, 1); // add, not set!
                                                }

                                                if(i < 3) {
                                                    // Set alarm for survey notification
                                                    alarmManager.setRepeating(
                                                            AlarmManager.RTC_WAKEUP,
                                                            cal.getTimeInMillis(),
                                                            alarmManager.INTERVAL_DAY,
                                                            notifPendingIntent);
                                                }
                                                else {
                                                    // Set alarm for survey diolog
                                                    alarmManager.setRepeating(
                                                            AlarmManager.RTC_WAKEUP,
                                                            cal.getTimeInMillis(),
                                                            alarmManager.INTERVAL_DAY,
                                                            dialogPendingIntent);
                                                }


                                                cal.add(Calendar.MINUTE, duration/4);
                                            }
                                        }

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
                                    }
                                }
                            });
                        }
                        SharedPreferences.Editor edit = prefs.edit();
                        edit.putBoolean(getString(R.string.prev_started), Boolean.TRUE);
                        edit.commit();
                    }
                    else {
                        Log.i("Updates", "Parse initial survey request failed");
                    }
                }
            });
        }
    }
}
