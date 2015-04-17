package com.example.natalie.android_wellbeing;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

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
        Log.i("Notification>>>", "Notification Service handled");

        int id = intent.getIntExtra("ID", 1);

        dbHandler = new SurveyDatabaseHandler(getApplicationContext());
        dbHandler.setComplete(false, id);

        Intent i = new Intent(NotificationService.this, ReminderNotification.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("ID", id);
        startActivity(i);
    }
}
