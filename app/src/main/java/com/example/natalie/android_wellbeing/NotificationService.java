package com.example.natalie.android_wellbeing;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
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

        final int sid = intent.getIntExtra("SID", 1);
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

        if(!dbHandler.isCompleted(sid)) {
            if(iteration < 4) {
                String survey_name = dbHandler.getName(sid);

                Intent notificationIntent = new Intent(this, Checkpoint.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                notificationIntent.putExtra("ID", sid);
                notificationIntent.setAction(String.valueOf(sid));
                PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0);

                Log.i("DEBUG>>>", "In notification, ID for " + survey_name + " = " + String.valueOf(sid));
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("WellBeing")
                        .setContentText("Ready to take your " + survey_name + " survey?")
                        .setDefaults(android.app.Notification.DEFAULT_LIGHTS |
                                android.app.Notification.DEFAULT_SOUND  |
                                android.app.Notification.DEFAULT_VIBRATE)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                // notificationID allows you to update the notification later on.
                mNotificationManager.notify(sid, mBuilder.build());
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
