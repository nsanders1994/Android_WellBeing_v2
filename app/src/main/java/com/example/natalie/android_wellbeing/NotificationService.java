package com.example.natalie.android_wellbeing;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import java.util.Calendar;

/**
 * Created by Natalie on 4/10/2015.
**/

public class NotificationService extends IntentService {
    /**
     * This service is called after a survey alarm is triggered. It sets the next alarm/s necessary
     * and notifies the user that a survey is open
    **/

    private SurveyDatabaseHandler dbHandler; // handler for the SQLite database

    public NotificationService() {
        super("Service");
    }

    @Override
    public void onCreate() {super.onCreate();}

    @Override
    protected void onHandleIntent(Intent intent) {

        // Retrieve extras from the intent
        final int ID  = intent.getIntExtra("ID", 1);        // survey ID
        String partID = intent.getStringExtra("PART_ID");   // partial ID of the pending intent (survey ID + day + time index)
        int iteration = intent.getIntExtra("ITER", 1);      // alarm iteration (1-4 for every active period)
        String time   = intent.getStringExtra("TIME");      // start time of active period

        // Retrieve the duration of this survey's active periods
        dbHandler = new SurveyDatabaseHandler(getApplicationContext());
        int duration = dbHandler.getDuration(ID);

        // If the current time is not valid for the requested survey (an update may have occurred),
        // exit the service
        if(!Utilities.validTime(getApplicationContext(),ID)){
            return;
        }

        // If this is the first notification for the active period...
        if(iteration == 1){

            // As the first notification, set survey_completed to False and clear the answer and
            // timestamp lists in the database to start off the new active period
            dbHandler.setComplete(false, ID);
            dbHandler.storeTStamps("empty", ID);
            dbHandler.storeAnswers("empty", ID);

            // Start the starting alarm for this active period for the next week
            int hr = Integer.parseInt(time.split(":")[0]);
            int min = Integer.parseInt(time.split(":")[1]);
            Calendar cal = Calendar.getInstance();

            cal.set(Calendar.HOUR_OF_DAY, hr);
            cal.set(Calendar.MINUTE, min);
            cal.set(Calendar.SECOND, 0);
            cal.add(Calendar.DAY_OF_WEEK, 7);

            int intentID = Integer.parseInt(partID + String.valueOf(1));

            Intent notifIntent = new Intent(getApplicationContext(), NotificationService.class);
            notifIntent.putExtra("ID", ID);              // survey id
            notifIntent.putExtra("PART_ID", partID);     // notification id without the iteration number
            notifIntent.putExtra("ITER", 1);             // iteration number
            notifIntent.putExtra("TIME", time);          // start time

            PendingIntent notifPendingIntent = PendingIntent.getService(
                    getApplicationContext(),
                    intentID,
                    notifIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    cal.getTimeInMillis(),
                    notifPendingIntent);
        }

        // Set next iteration of today's alarm
        if(iteration < 4 && !dbHandler.isCompleted(ID)) {
            int intentID = Integer.parseInt(partID + String.valueOf(iteration + 1));

            Intent notifIntent = new Intent(getApplicationContext(), NotificationService.class);
            notifIntent.putExtra("ID", ID);              // survey id
            notifIntent.putExtra("PART_ID", partID);     // notification id without the iteration number
            notifIntent.putExtra("ITER", iteration + 1); // iteration number
            notifIntent.putExtra("TIME", time);          // start time

            PendingIntent notifPendingIntent = PendingIntent.getService(
                    getApplicationContext(),
                    intentID,
                    notifIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + (duration/4)*60*1000, notifPendingIntent);
        }

        // Show current notification/dialog
        if(!dbHandler.isCompleted(ID) && Utilities.validTime(getApplicationContext(), ID)) {
            // If it is iteration 1, 2, or 3, send a notification
            if(iteration < 4) {

                String survey_name = dbHandler.getName(ID); // survey name

                // If the notification is clicked, the app will call the Checkpoint service
                Intent notificationIntent = new Intent(this, Checkpoint.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                notificationIntent.putExtra("ID", ID);
                notificationIntent.setAction(String.valueOf(ID));
                PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0);

                // Build and display notification
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

                // The notificationID allows you to update the notification later on.
                mNotificationManager.notify(ID, mBuilder.build());

            }
            // Otherwise, send a user dialog
            else if(iteration == 4 && Utilities.validTime(getApplicationContext(), ID)){
                Intent i = new Intent(NotificationService.this, ReminderDialog.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra("ID", ID);
                startActivity(i);
            }
        }
    }
}
