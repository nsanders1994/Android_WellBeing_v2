package com.example.natalie.android_wellbeing;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;
import java.util.List;

/**
 * Created by Natalie on 12/17/2014.
 */
public class OnBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        // Retrieve number of surveys from database
        final SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(context);
        List<Integer> survey_ids = dbHandler.getSurveyIDs();

        // Create a repeating alarm for popup for every survey at the specified time
        for (int k = 0; k < survey_ids.size(); k++) {

            int curr_id = survey_ids.get(k);
            List<String> times = dbHandler.getQuesTypes(curr_id);
            int duration = dbHandler.getDuration(curr_id);

            for (int j = 0; j < times.size(); j++) {
                int hr = Integer.parseInt(String.valueOf(times.get(j)).split(":")[0]);
                int min = Integer.parseInt(String.valueOf(times.get(j)).split(":")[1]);

                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, hr);
                cal.set(Calendar.MINUTE, min);
                cal.set(Calendar.SECOND, 0);

                for (int i = 0; i < 4; i++) {
                    // Alarm will trigger the pop-up dialog
                    PendingIntent pendingIntent = PendingIntent.getService(
                            context,
                            Integer.parseInt(String.valueOf(curr_id) + String.valueOf(i)),
                            new Intent(context, PopupService.class),
                            PendingIntent.FLAG_CANCEL_CURRENT);

                    // Get alarm time
                    int curr_hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    int curr_min = Calendar.getInstance().get(Calendar.MINUTE);

                    // If it's after the alarm time, schedule for next day
                    if (curr_hr > hr || curr_hr == cal.get(Calendar.HOUR_OF_DAY)
                            && curr_min > cal.get(Calendar.MINUTE)) {
                        cal.add(Calendar.DAY_OF_YEAR, 1); // add, not set!
                    }

                    // Set alarm for survey pop-up to go off at default of 8:00 AM
                    alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            cal.getTimeInMillis(),
                            alarmManager.INTERVAL_DAY,
                            pendingIntent);

                    cal.add(Calendar.MINUTE, duration / 4);
                }
            }
            /*
            ////////////////////////////////////////////////////////////////
            Intent popup_intent = new Intent(context, PopupService.class);
            popup_intent.putExtra("ID", curr_id);

            PendingIntent pendingIntent = PendingIntent.getService(
                    context,
                    curr_id,
                    popup_intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            // Set time for survey pop-up
            Calendar cal = Calendar.getInstance();

            // Get alarm time
            int hr = dbHandler.getHr(curr_id);
            int min = dbHandler.getMin(curr_id);
            int curr_hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            int curr_min = Calendar.getInstance().get(Calendar.MINUTE);

            // If it's after the alarm time, schedule for next day
            if ( curr_hr > hr || curr_hr == hr && curr_min > min) {
                cal.add(Calendar.DAY_OF_YEAR, 1); // add, not set!
            }

            cal.set(Calendar.HOUR_OF_DAY, hr);
            cal.set(Calendar.MINUTE, min);
            cal.set(Calendar.SECOND, 0);

            // Set alarm for survey pop-up to go off
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    cal.getTimeInMillis(),
                    alarmManager.INTERVAL_DAY,
                    pendingIntent);
        }

        // Start alarm for a service which checks for updates
        PendingIntent pendingIntent2 = PendingIntent.getService(
                context,
                -1,
                new Intent(context, UpdateService.class),
                PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager2 = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(Calendar.HOUR_OF_DAY, 0);
        cal2.set(Calendar.MINUTE, 0);

        alarmManager2.setInexactRepeating(
                AlarmManager.RTC,
                cal2.getTimeInMillis(),
                alarmManager2.INTERVAL_DAY,
                pendingIntent2);*/
        }
    }
}