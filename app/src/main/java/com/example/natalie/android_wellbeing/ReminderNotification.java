package com.example.natalie.android_wellbeing;

import android.app.Activity;

/**
 * Created by Natalie on 4/10/2015.
 */
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ReminderNotification extends Activity {
    int ID = 0;
    SurveyDatabaseHandler dbHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get survey ID from caller intent
        Intent caller = getIntent();
        ID = caller.getIntExtra("ID", 1);

        dbHandler = new SurveyDatabaseHandler(getApplicationContext());
        String survey_name = dbHandler.getName(ID);

        Intent notificationIntent = new Intent(this, Checkpoint.class);
        notificationIntent.putExtra("ID", ID);
        notificationIntent.setAction(String.valueOf(ID));
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0);

        Log.i("DEBUG>>>", "In notification, ID for " + survey_name + " = " + String.valueOf(ID));
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
        mNotificationManager.notify(ID, mBuilder.build());
    }
}
