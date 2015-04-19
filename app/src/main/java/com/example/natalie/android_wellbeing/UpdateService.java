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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
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

    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_FINISHED = 1;
    public static final int STATUS_ERROR = 2;
    Intent intent;
    Bundle bundle = new Bundle();


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
        final Object parseSync  = new Object();
        final Object notifySync = new Object();

        final ResultReceiver receiver = intent.getParcelableExtra("receiver");
        final SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());

        int svyCt = dbHandler.getSurveyCount();
        List<Integer> IDs = dbHandler.getSurveyIDs();

        for(int j = 0; j < svyCt; j++) { // for all surveys
            int survey_id = IDs.get(j);
            int timeCt = dbHandler.getTimes(j + 1).size();
            int dayCt  = dbHandler.getDays(j + 1).size();

            for(int d = 0; d < dayCt; d++){
                for(int k = 0; k < timeCt; k++) { // for all times that survey is available
                    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                    Intent dialogIntent = new Intent(getApplicationContext(), PopupService.class);
                    dialogIntent.putExtra("ID", j);

                    for(int i = 1; i <= 4; i++) {
                        String partID = String.valueOf(survey_id) + // survey id
                                String.valueOf(d) +                 // day
                                String.valueOf(j);                  // time

                        int intentID = Integer.parseInt(partID + String.valueOf(i));

                        Intent notifIntent;
                        notifIntent = new Intent(getApplicationContext(), NotificationService.class);
                        notifIntent.putExtra("ID", survey_id);
                        notifIntent.putExtra("PART_ID", partID);
                        notifIntent.putExtra("ITER", i);

                        PendingIntent notifPendingIntent = PendingIntent.getService(
                                getApplicationContext(),
                                intentID,
                                notifIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT);

                        // Cancel alarms
                        try {
                            alarmManager.cancel(notifPendingIntent);
                        } catch (Exception e) {
                            Log.e("ERROR>>> ", "AlarmManager update was not canceled. " + e.toString());
                        }
                    }
                }
            }
        }

        Log.i("DEBUG>>>", "Before " + dbHandler.getSurveyIDs().toString());

        // Clear Survey Data
        dbHandler.deleteAll();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        Log.i("DEBUG>>>", "Parse Query Starting");
        receiver.send(STATUS_RUNNING, Bundle.EMPTY);

        // Get all surveys
        final ParseQuery<ParseObject> query = new ParseQuery<>("SurveySummary");
        query.whereEqualTo("Active", true);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> all_surveys, ParseException e) {
                if (e == null) {
                    final int surveyCt = all_surveys.size();
                    bundle.putInt("SURVEYCT", surveyCt);

                    for (int i = 0; i < surveyCt; i++) {
                        //final int index = i;

                        ParseObject survey_listing = all_surveys.get(i);
                        final String name = survey_listing.getString("Category");
                        final List<Object> times = survey_listing.getList("Time");
                        final int duration = survey_listing.getInt("surveyActiveDuration");
                        final String table_name = survey_listing.getString("Survey");
                        final int surveyVersion = survey_listing.getInt("Version");
                        final List<Object> days = survey_listing.getList("Days");

                        // Get list of questions and their answers
                        ParseQuery<ParseObject> query2 = new ParseQuery<ParseObject>(table_name);
                        query2.orderByAscending("questionId");
                        query2.findInBackground(new FindCallback<ParseObject>() {
                            public void done(List<ParseObject> survey, ParseException e) {
                                if (e == null) {
                                    int ques_ct = survey.size();
                                    List<Object> ques = new ArrayList<>(ques_ct);
                                    List<Object> ans = new ArrayList<>(ques_ct);
                                    List<Object> type = new ArrayList<>(ques_ct);
                                    List<Object> ansVals = new ArrayList<>(ques_ct);

                                    for (int j = 0; j < ques_ct; j++) {
                                        ParseObject curr_ques = survey.get(j);

                                        type.add(curr_ques.getString("questionType"));
                                        ques.add(curr_ques.getString("question"));
                                        ans.add(Utilities.join(curr_ques.getList("options"), "%%"));
                                        ansVals.add(Utilities.join(curr_ques.getList("numericScale"), "%%"));
                                    }

                                    String ques_str = Utilities.join(ques, "%%");
                                    String type_str = Utilities.join(type, "%%");
                                    String ans_str = Utilities.join(ans, "%nxt%");
                                    String ansVal_str = Utilities.join(ansVals, "%nxt%");

                                    // Store Survey
                                    dbHandler.createSurvey(
                                            Utilities.join(times, ","),
                                            duration,
                                            name,
                                            ques_str,
                                            ans_str,
                                            type_str,
                                            ansVal_str,
                                            surveyVersion,
                                            Utilities.join(days, ",")
                                    );

                                    // Get current survey's ID in the database
                                    int survey_id = dbHandler.getLastRowID();

                                    // Prepare to set alarms
                                    int dayCt = days.size();
                                    int timeCt = times.size();
                                    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                                    // Set Alarms for all days
                                    for (int d = 0; d < dayCt; d++) {
                                        int currDay = Integer.parseInt(String.valueOf(days.get(d))) + 1; // days are give 0-6, android uses 1-7

                                        // Set first alarm of each active period for all survey times
                                        for (int j = 0; j < timeCt; j++) {
                                            int hr = Integer.parseInt(String.valueOf(times.get(j)).split(":")[0]);
                                            int min = Integer.parseInt(String.valueOf(times.get(j)).split(":")[1]);

                                            // Get curr time
                                            Calendar curr_cal = Calendar.getInstance();

                                            // Get alarm time
                                            Calendar cal = Calendar.getInstance();
                                            cal.set(Calendar.DAY_OF_WEEK, currDay);
                                            cal.set(Calendar.HOUR_OF_DAY, hr);
                                            cal.set(Calendar.MINUTE, min);
                                            cal.set(Calendar.SECOND, 0);

                                            // If it's after the alarm time, schedule starting alarm for next day
                                            if (curr_cal.getTimeInMillis() > cal.getTimeInMillis()) {
                                                cal.add(Calendar.DAY_OF_YEAR, 7); // add, not set!

                                                // Check if any of the following alarms for this active period are not past
                                                for (int k = 1; k < 4; k++) {
                                                    // Get alarm time
                                                    Calendar cal2 = Calendar.getInstance();
                                                    cal2.set(Calendar.DAY_OF_WEEK, currDay);
                                                    cal2.set(Calendar.HOUR_OF_DAY, hr);
                                                    cal2.set(Calendar.MINUTE, min + k * (duration / 4));
                                                    cal2.set(Calendar.SECOND, 0);

                                                    // If there is an iteration that isn't past the current time, schedule alarm
                                                    if (curr_cal.getTimeInMillis() < cal2.getTimeInMillis()) {
                                                        int iter = k + 1;
                                                        String partID = String.valueOf(survey_id) + // survey id
                                                                String.valueOf(d) +                 // day
                                                                String.valueOf(j);                  // time

                                                        int intentID = Integer.parseInt(partID + String.valueOf(iter));

                                                        Intent intent;
                                                        intent = new Intent(getApplicationContext(), NotificationService.class);
                                                        intent.putExtra("ID", survey_id);
                                                        intent.putExtra("PART_ID", partID);
                                                        intent.putExtra("ITER", iter);

                                                        PendingIntent notifPendingIntent2 = PendingIntent.getService(
                                                                getApplicationContext(),
                                                                intentID,
                                                                intent,
                                                                PendingIntent.FLAG_CANCEL_CURRENT);

                                                        // Set secondary alarm for survey notification
                                                        alarmManager.set(AlarmManager.RTC_WAKEUP, cal2.getTimeInMillis(), notifPendingIntent2);

                                                        break;
                                                    }
                                                }
                                            }
                                            Log.i("SYNC>>>", ">>TEST<<");
                                            String partID = String.valueOf(d) + String.valueOf(survey_id) + String.valueOf(j);
                                            int intentID = Integer.parseInt(partID + "1");

                                            Intent notifIntent = new Intent(getApplicationContext(), NotificationService.class);
                                            notifIntent.putExtra("ID", survey_id);
                                            notifIntent.putExtra("PART_ID", partID);
                                            notifIntent.putExtra("ITER", 1);

                                            PendingIntent notifPendingIntent = PendingIntent.getService(
                                                    getApplicationContext(),
                                                    intentID,
                                                    notifIntent,
                                                    PendingIntent.FLAG_CANCEL_CURRENT);

                                            // Set alarm for survey notification
                                            alarmManager.setRepeating(
                                                    AlarmManager.RTC_WAKEUP,
                                                    cal.getTimeInMillis(),
                                                    7 * alarmManager.INTERVAL_DAY,
                                                    notifPendingIntent);
                                        }
                                    }
                                }
                                Log.i("SYNC>>>", "Sending finished");
                                receiver.send(STATUS_FINISHED, bundle);
                            }
                        });
                    }

                    /*Log.i("SYNC>>>", "notifySync.notify");
                    synchronized(notifySync){
                        notifySync.notify();
                    }*/

                } else {
                    Log.i("DEBUG>>>", "Parse initial survey request failed");
                    bundle.putString(Intent.EXTRA_TEXT, e.toString());
                    receiver.send(STATUS_ERROR, bundle);
                }
            }
        });

        /*Log.i("SYNC>>>", "notifySync.wait");
        synchronized (notifySync) {
            try {
                notifySync.wait();
            } catch (InterruptedException ex) {
                Log.i("DEBUG>>>", "syncObject.wait() throws " + ex.toString());
            }
        }*/



        //receiver.send(STATUS_FINISHED, bundle);

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

}
