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
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

/**
 * Created by Natalie on 2/15/2015.
 */
public class UpdateService extends IntentService {

    public static final String BROADCAST_ACTION = "com.example.natalie.android_wellbeing.update";
    private final Handler handler = new Handler();
    Intent intent;
    private LocalBroadcastManager broadcaster;

    public UpdateService() {
        super("Service");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //broadcaster = LocalBroadcastManager.getInstance(this);

    }

    /*@Override
    protected void onHandleIntent(Intent intent) {
        handler.removeCallbacks(sendUpdatesToUI);
        handler.post(sendUpdatesToUI);//postDelayed(sendUpdatesToUI, 1000); // 1 second
    }

    private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {
            UpdateUI();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(sendUpdatesToUI);
        super.onDestroy();
    }*/

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        Log.i("DEBUG>>>", "FirstService destroyed");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i("DEBUG>>>>>", "IN UPDATE");

        final SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());

        int count = dbHandler.getSurveyCount();
        Log.i("DEBUG>>", "survey ct in update = " + String.valueOf(count));

        List<Integer> IDs = dbHandler.getSurveyIDs();

        for(int j = 0; j < count; j++) { // for all surveys
            Log.i("DEBUG>>>", "In 1st for loop");
            int curr_surveyTimeCt = dbHandler.getTimes(j + 1).size();
            Log.i("DEBUG>>>", "curr_surveyTimeCt = " + String.valueOf(curr_surveyTimeCt));

            for(int k = 0; k < curr_surveyTimeCt; k++) { // for all times that survey is available
                Log.i("DEBUG>>>", "In 2nd for loop");
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                Intent dialogIntent = new Intent(getApplicationContext(), PopupService.class);
                dialogIntent.putExtra("ID", j);

                Intent notifIntent = new Intent(getApplicationContext(), PopupService.class);
                notifIntent.putExtra("ID", j);

                for(int i = 0; i < 4; i++) {
                    PendingIntent dialogPendingIntent = PendingIntent.getService(
                            getApplicationContext(),
                            Integer.parseInt(String.valueOf(IDs.get(j)) + String.valueOf(k) + String.valueOf(i)),
                            dialogIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT);

                    PendingIntent notifPendingIntent = PendingIntent.getService(
                            getApplicationContext(),
                            Integer.parseInt(String.valueOf(IDs.get(j)) + String.valueOf(k) + String.valueOf(i)),
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

        }
        Log.i("DEBUG>>>", "Before " + dbHandler.getSurveyIDs().toString());

        // Clear Survey Data
        dbHandler.deleteAll();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor edit1 = prefs.edit();
        edit1.putBoolean(getString(R.string.update), Boolean.TRUE);
        edit1.commit();
        SharedPreferences.Editor edit2 = prefs.edit();
        edit2.putBoolean(getString(R.string.importActive), Boolean.TRUE);
        edit2.commit();


        Log.i("DEBUG>>>", "Parse Query Starting");
        final ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("SurveySummary");
        query.whereEqualTo("Active", true);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> all_surveys, ParseException e) {
                if (e == null) {
                    Log.i("DEBUG>>>", "In loop!");
                    for (int i = 0; i < all_surveys.size(); i++) {

                        final int index = i + 1;
                        final int end   = all_surveys.size();
                        ParseObject survey_listing = all_surveys.get(i);
                        final String name          = survey_listing.getString("Category");
                        final List<Object> time    = survey_listing.getList("Time");
                        final int duration         = survey_listing.getInt("surveyActiveDuration");
                        final String table_name    = survey_listing.getString("Survey");
                        final int surveyVersion    = survey_listing.getInt("Version");

                        // Get list of questions and their answers
                        ParseQuery<ParseObject> query2 = ParseQuery.getQuery(table_name);
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
                                        int qID = curr_ques.getInt("questionId") - 1;

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

                                    Log.i("DEBUG>>>", "# times = " + String.valueOf(time.size()));
                                    for(int j = 0; j < time.size(); j++) {
                                        int hr  = Integer.parseInt(String.valueOf(time.get(j)).split(":")[0]);
                                        int min = Integer.parseInt(String.valueOf(time.get(j)).split(":")[1]);

                                        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                                        int inc = 0;
                                        for(int i = 0; i < 4; i++){
                                            // Get alarm time
                                            Calendar cal = Calendar.getInstance();
                                            cal.set(Calendar.HOUR_OF_DAY, hr);
                                            cal.set(Calendar.MINUTE, min + inc);
                                            cal.set(Calendar.SECOND, 0);

                                            // Get curr time
                                            Calendar curr_cal = Calendar.getInstance();

                                            // If it's after the alarm time, schedule for next day
                                            if ( curr_cal.getTimeInMillis() > cal.getTimeInMillis()) {

                                                Log.i("DEBUG>>>", "Survey alarm scheduled for next day");
                                                cal.add(Calendar.DAY_OF_YEAR, 1); // add, not set!
                                            }

                                            Log.i("DEBUG>>>", "Alarm time " + String.valueOf(i) +
                                                    " = " + String.valueOf(cal.get(Calendar.HOUR)) + ":" +
                                                    String.valueOf(cal.get(Calendar.MINUTE)));
                                            if(i < 3) {
                                                Intent notifIntent = new Intent(getApplicationContext(), NotificationService.class);
                                                notifIntent.putExtra("ID", survey_id);

                                                Log.i("DEBUG>>>", "NOTIF:" + String.valueOf(survey_id) + String.valueOf(j) + String.valueOf(i));
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
                                                Log.i("DEBUG>>>", "DIALOG:" + String.valueOf(survey_id) + String.valueOf(j) + String.valueOf(i));
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

                                if(index == end){
                                    Log.i("DEBUG>>>", "Setting Preferences: importFinished == true");
                                    SharedPreferences.Editor edit = prefs.edit();
                                    edit.putBoolean(getString(R.string.importActive), Boolean.FALSE);
                                    edit.commit();
                                }
                            }
                        });
                    }
                }
                else {
                    Log.i("DEBUG>>>", "Parse initial survey request failed");
                }
            }
        });

        /*
        while(!prefs.getBoolean(getString(R.string.importFinished), Boolean.FALSE)){
            try{
                Thread.sleep(50);
            }
            catch(java.lang.InterruptedException ex)
            {
                Log.i("DEBUG>>>", ex.toString());
            }
        }

        Log.i("DEBUG>>>", "new survey ids = " + dbHandler.getSurveyIDs().toString());
        Log.i("DEBUG>>>", "new names = " + dbHandler.getName(1) + " " + dbHandler.getName(2));

        sendBroadcast(intent);
        refreshStartPage();
        */
    }

    public void refreshStartPage() {
        intent = new Intent(BROADCAST_ACTION);
        this.sendBroadcast(intent);
    }

}
