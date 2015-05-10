package com.example.natalie.android_wellbeing;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.ResultReceiver;
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
    public ResultReceiver receiver;
    Intent intent;
    Bundle bundle = new Bundle();

    public UpdateService() {
        super("Service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Initialize the SQLite database handler
        final SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());

        // Check whether the update service was called from the OnBoot service; if so no information
        // needs to be sent to the broadcast receiver
        final boolean from_boot = intent.getBooleanExtra("FROM_BOOT", false);

        if(!from_boot){
            receiver = intent.getParcelableExtra("receiver");
        }

        // Do not attempt to update if there is not WiFi
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (!mWifi.isConnected()) {
            Log.i("WIFI>>>", "WiFi not connected");
            return;
        }

        // Close any user dialog notifications that might still be open
        Intent dialogClose = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        sendBroadcast(dialogClose);

        // Clear Survey Data
        dbHandler.deleteAll();

        int x = dbHandler.getSurveyCount();
        Log.i("DEBUG>>>", "number of surveys after clearing = " + String.valueOf(x));

        Log.i("DEBUG>>>", "Parse Query Starting");
        if(!from_boot) receiver.send(STATUS_RUNNING, Bundle.EMPTY);

        // Import all active surveys from Parse
        final ParseQuery<ParseObject> query = new ParseQuery<>("SurveySummary");
        query.whereEqualTo("Active", true);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> all_surveys, ParseException e) {
                if (e == null) {
                    final int surveyCt = all_surveys.size(); // total number of surveys to import
                    bundle.putInt("SURVEYCT", surveyCt);     // add survey count to bundle to be sent
                                                             // to the broadcast receiver in StartScreen

                    // For all surveys...
                    for (int i = 0; i < surveyCt; i++) {
                        // Get all data for the current survey
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
                                    List<Object> endpts = new ArrayList<>(ques_ct);

                                    for (int j = 0; j < ques_ct; j++) {
                                        ParseObject curr_ques = survey.get(j);

                                        type.add(curr_ques.getString("questionType"));
                                        ques.add(curr_ques.getString("question"));

                                        if(curr_ques.getString("questionType").equals("Textbox")){
                                            ans.add("NA");
                                            ansVals.add(-1);
                                        }
                                        else{
                                            ans.add(Utilities.join(curr_ques.getList("options"), "`"));
                                            ansVals.add(Utilities.join(curr_ques.getList("numericScale"), "`"));
                                        }

                                        List<Object> temp = curr_ques.getList("endPoints");
                                        if(temp.size() != 0){
                                            endpts.add(Utilities.join(temp, "`"));
                                        }else{
                                            endpts.add("-`-");
                                        }
                                    }



                                    String ques_str = Utilities.join(ques, "`");
                                    String type_str = Utilities.join(type, "`");
                                    String ans_str = Utilities.join(ans, "`nxt`");
                                    String ansVal_str = Utilities.join(ansVals, "`nxt`");
                                    String endPts_str = Utilities.join(endpts, "`nxt`");

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
                                            Utilities.join(days, ","),
                                            endPts_str
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
                                                        intent.putExtra("TIME", String.valueOf(hr) + ":" + String.valueOf(min));

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
                                                    //7 * alarmManager.INTERVAL_DAY,
                                                    notifPendingIntent);
                                        }
                                    }
                                }
                                Log.i("SYNC>>>", "Sending finished");
                                if(!from_boot) receiver.send(STATUS_FINISHED, bundle);
                            }
                        });
                    }

                } else {
                    Log.i("DEBUG>>>", "Parse initial survey request failed");
                    bundle.putString(Intent.EXTRA_TEXT, e.toString());
                    if(!from_boot) receiver.send(STATUS_ERROR, bundle);
                }
            }
        });
    }
}
