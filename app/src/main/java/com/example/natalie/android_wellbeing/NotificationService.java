package com.example.natalie.android_wellbeing;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by Natalie on 4/10/2015.
 */
public class NotificationService extends IntentService {
    private SurveyDatabaseHandler dbHandler;

    public NotificationService() {
        super("Service");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        int sid = intent.getIntExtra("SID", 1);
        int tid = intent.getIntExtra("TID", 1);
        int iteration = intent.getIntExtra("ITER", 1);
        String tCurr = intent.getStringExtra("T_CURR");

        dbHandler = new SurveyDatabaseHandler(getApplicationContext());
        int duration = dbHandler.getDuration(sid);

        Log.i("Notification>>>", "NOTIFICATION SERVICE FOR " + String.valueOf(sid));

        // Set first alarm for next day

        if(iteration == 1) {
            // As the first notification, set survey_completed to False and start new active period
            dbHandler.setComplete(false, sid);
            Log.i("DEBUG>>>", "Reset setComplete = false for "  + String.valueOf(sid));
        }

        Log.i("DEBUG>>>", "Iteration for " + String.valueOf(sid) + " = " + String.valueOf(iteration));

        // Set next iteration of today's alarm
        if(iteration < 4 && !dbHandler.isCompleted(sid)) {
            Log.i("DEBUG>>>", "Set notification alarm for " + String.valueOf(sid));

            Intent notifIntent = new Intent(getApplicationContext(), NotificationService.class);
            notifIntent.putExtra("TID", tid);
            notifIntent.putExtra("SID", sid);
            notifIntent.putExtra("T_CURR", tCurr);
            notifIntent.putExtra("ITER", iteration + 1);

            PendingIntent notifPendingIntent = PendingIntent.getService(
                    getApplicationContext(),
                    // alarm id code is <survey id> <survey time id> <iteration of survey time>
                    Integer.parseInt(String.valueOf(sid) + String.valueOf(tid) + String.valueOf(iteration + 1)),
                    notifIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + (duration/4)*60*1000, notifPendingIntent);
        }
        /*else if(iteration == 3 && !dbHandler.isCompleted(sid)){
            Log.i("DEBUG>>>", "Set dialog alarm for " + String.valueOf(sid));

            Intent dialogIntent = new Intent(getApplicationContext(), PopupService.class);
            dialogIntent.putExtra("ID", sid);

            PendingIntent dialogPendingIntent = PendingIntent.getService(
                    getApplicationContext(),
                    // alarm id code is <survey id> <survey time id> <iteration of survey time>
                    Integer.parseInt(String.valueOf(sid) + String.valueOf(tid) + String.valueOf(iteration + 1)),
                    dialogIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + (duration/4)*60*1000, dialogPendingIntent);
        }*/


        if(!dbHandler.isCompleted(sid)) {
            if(iteration < 4) {
                Log.i("DEBUG>>>", "Go to Notification for " + String.valueOf(sid));
                Intent i = new Intent(NotificationService.this, ReminderNotification.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra("ID", sid);
                startActivity(i);
            }
            else if(iteration == 4){
                Log.i("DEBUG>>>", "Go to Dialog for " + String.valueOf(sid));
                Intent i = new Intent(NotificationService.this, ReminderDialog.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra("ID", sid);
                startActivity(i);
            }
        }
    }
}
