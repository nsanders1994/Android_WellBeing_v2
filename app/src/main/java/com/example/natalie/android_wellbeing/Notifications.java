package com.example.natalie.android_wellbeing;

import android.app.Activity;
import android.app.Notification;

/**
 * Created by Natalie on 4/10/2015.
 */
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.TaskStackBuilder;

public class Notifications extends Activity {
    private int ID = 0;
    private int iteration = 0;
    private static final int NOTIFY_ME_ID=1337;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get survey ID from caller intent
        Intent caller = getIntent();
        ID = caller.getIntExtra("ID", 1);
        iteration = caller.getIntExtra("ITERATION", 0);

        long[] pattern = {500,500,500,500,500,500,500,500,500};

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("WellBeing Survey")
                .setContentText("Hello World!")
                .setAutoCancel(true)
                .setLights(Color.BLUE, 500, 500)
                .setVibrate(pattern);

        // Add intent
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(SurveyScreen.class);

        Intent intent = new Intent(this, SurveyScreen.class);
        intent.putExtra("ID", ID);

        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // notificationID allows you to update the notification later on.
        mNotificationManager.notify(ID, mBuilder.build());
    }
}
