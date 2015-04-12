package com.example.natalie.android_wellbeing;

import android.app.IntentService;
import android.content.Intent;

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
        int table_id = intent.getIntExtra("ID", 1);
        int iteration = intent.getIntExtra("ITERATION", 0);

        dbHandler.setComplete(false, table_id);

        Intent i = new Intent(NotificationService.this, Notifications.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("ID", table_id);
        i.putExtra("ITERATION", iteration);
        startActivity(i);
    }
}
