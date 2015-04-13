package com.example.natalie.android_wellbeing;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

/**
 * Created by Natalie on 2/15/2015.
 */
public class UpdateService extends IntentService {
    public UpdateService() {
        super("Service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());
        dbHandler.deleteAll();

        List<Integer> IDs = dbHandler.getSurveyIDs();
        int idCt = IDs.size();

        for(int j = 0; j < idCt; j++) { // for all surveys
            int curr_surveyTimeCt = dbHandler.getTimes(j).size();
            for(int k = 0; k < curr_surveyTimeCt; k++) { // for all times that survey is available
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                Intent dialogIntent = new Intent(getApplicationContext(), PopupService.class);
                dialogIntent.putExtra("ID", j);
                dialogIntent.putExtra("ITERATION", k);

                Intent notifIntent = new Intent(getApplicationContext(), PopupService.class);
                notifIntent.putExtra("ID", j);
                notifIntent.putExtra("ITERATION", k);

                PendingIntent dialogPendingIntent = PendingIntent.getService(
                        getApplicationContext(),
                        Integer.parseInt(String.valueOf(j) + String.valueOf(k)),
                        dialogIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT);

                PendingIntent notifPendingIntent = PendingIntent.getService(
                        getApplicationContext(),
                        Integer.parseInt(String.valueOf(j) + String.valueOf(k)),
                        dialogIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT);

                // Cancel alarms
                try {
                    alarmManager.cancel(notifPendingIntent);
                    alarmManager.cancel(dialogPendingIntent);

                } catch (Exception e) {
                    Log.e("ERROR>>> ", "AlarmManager update was not canceled. " + e.toString());
                }
            }

        }

        final ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("SurveySummary");

        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> all_surveys, ParseException e) {
                if (e == null) {
                    for (int i = 0; i < all_surveys.size(); i++) {
                        final int survey_id = i;

                        ParseObject survey_listing = all_surveys.get(i);
                        boolean active = survey_listing.getBoolean("Active");

                        if(!active) continue; // If the current survey is not active skip to the next survey

                        final String name = survey_listing.getString("Category");
                        final List<Object> time = survey_listing.getList("Time");
                        final int duration = survey_listing.getInt("surveyActiveDuration");
                        final String table_name = survey_listing.getString("Survey");
                        final int surveyVersion    = survey_listing.getInt("Version");

                        // Get list of questions and their answers
                        ParseQuery<ParseObject> query2 = new ParseQuery<ParseObject>(table_name);
                        query2.findInBackground(new FindCallback<ParseObject>() {
                            public void done(List<ParseObject> survey, ParseException e) {
                                if (e == null) {
                                    int ques_ct = survey.size();
                                    List<Object> ques = new ArrayList<>();
                                    List<Object> ans = new ArrayList<>();
                                    List<Object> type = new ArrayList<>();
                                    List<Object> ansVals = new ArrayList<>();

                                    for (int j = 0; j < ques_ct; j++) {
                                        ParseObject curr_ques = survey.get(j);
                                        int qID = curr_ques.getInt("questionId") - 1;

                                        type.set(qID, curr_ques.getString("questionType"));
                                        ques.set(qID, curr_ques.getString("question"));
                                        ans.set (qID, Utilities.join(curr_ques.getList("options"), "%%"));
                                        ansVals.set(qID, Utilities.join(curr_ques.getList("numericScale"), "%%"));
                                    }

                                    String ques_str = Utilities.join(ques, "%%");
                                    String type_str = Utilities.join(type, "%%");
                                    String ans_str = Utilities.join(ans, "%nxt%");
                                    String ansVal_str = Utilities.join(ansVals, "%%nxt%%");


                                    for(int j = 0; j < time.size(); j++) {
                                        int hr  = Integer.parseInt(String.valueOf(time.get(j)).split(":")[0]);
                                        int min = Integer.parseInt(String.valueOf(time.get(j)).split(":")[1]);

                                        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                                        Calendar cal = Calendar.getInstance();
                                        cal.set(Calendar.HOUR_OF_DAY, hr);
                                        cal.set(Calendar.MINUTE, min);
                                        cal.set(Calendar.SECOND, 0);

                                        for(int i = 0; i < 4; i++){
                                            // Alarm will trigger the pop-up dialog
                                            PendingIntent pendingIntent = PendingIntent.getService(
                                                    getApplicationContext(),
                                                    Integer.parseInt(String.valueOf(survey_id) + String.valueOf(i)),
                                                    new Intent(getApplicationContext(), PopupService.class),
                                                    PendingIntent.FLAG_CANCEL_CURRENT);

                                            // Get alarm time
                                            int curr_hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                                            int curr_min = Calendar.getInstance().get(Calendar.MINUTE);

                                            // If it's after the alarm time, schedule for next day
                                            if ( curr_hr > hr || curr_hr == cal.get(Calendar.HOUR_OF_DAY)
                                                    && curr_min > cal.get(Calendar.MINUTE)) {
                                                cal.add(Calendar.DAY_OF_YEAR, 1); // add, not set!
                                            }

                                            // Set alarm for survey pop-up to go off at default of 8:00 AM
                                            alarmManager.setRepeating(
                                                    AlarmManager.RTC_WAKEUP,
                                                    cal.getTimeInMillis(),
                                                    alarmManager.INTERVAL_DAY,
                                                    pendingIntent);

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
                }
                else {
                    Log.i("Updates", "Parse initial survey request failed");
                }
            }
        });

    }

    public void set_DialogAlarm(int hr, int min, int table_id) {

        Intent intent = new Intent(UpdateService.this, PopupService.class);
        intent.putExtra("ID", table_id);

        // Alarm is reset at new time
        PendingIntent pendingIntent = PendingIntent.getService(
                getApplicationContext(),
                table_id,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Set new time
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hr);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, 0);

        // Set alarm for survey pop-up
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.getTimeInMillis(),
                alarmManager.INTERVAL_DAY,
                pendingIntent);
    }
}
