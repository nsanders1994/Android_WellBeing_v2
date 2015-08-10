package com.example.natalie.android_wellbeing;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * Created by Natalie on 12/17/2014.
**/

public class OnBoot extends BroadcastReceiver {
    /**
     * When the device shuts down all pending intents are lost, and thus all survey alarms. This class
     * resets all the survey alarms and the alarm for the update service
    **/

    @Override
    public void onReceive(Context context, Intent intent) {
        // Set alarm for the update service
        start_UpdatesService(context);

        // Initialize the SQLit database and an alarm manager
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(context);

        // Retrive all survey IDs from the database
        List<Integer> survey_ids = dbHandler.getSurveyIDs();

        // For every survey set the an alarm for each of its active periods
        for (int k = 0; k < survey_ids.size(); k++) {
            int survey_id = survey_ids.get(k);                  // current survey ID
            List<String> times = dbHandler.getTimes(survey_id); // all start times for the current survey
            List<Integer> days = dbHandler.getDays(survey_id);  // all days survey should have active periods
            int dayCt = days.size();                            // number of days with active periods
            int timeCt = times.size();                          // number of active periods
            int duration = dbHandler.getDuration(survey_id);    // survey duration

            // For every day a survey can be active...
            for(int d = 0; d < dayCt; d++) {

                // The current day; days are give 0-6, android uses 1-7
                int currDay = Integer.parseInt(String.valueOf(days.get(d))) + 1;

                if (currDay == 0) break; // 24-hr survey

                // Set first alarm for all survey times
                for(int j = 0; j < timeCt; j++) {
                    int hr  = Integer.parseInt(String.valueOf(times.get(j)).split(":")[0]); // start time hour
                    int min = Integer.parseInt(String.valueOf(times.get(j)).split(":")[1]); // start time minute

                    // Get current time calendar
                    Calendar curr_cal = Calendar.getInstance();

                    // Create alarm time calendar
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.DAY_OF_WEEK, currDay);
                    cal.set(Calendar.HOUR_OF_DAY, hr);
                    cal.set(Calendar.MINUTE, min);
                    cal.set(Calendar.SECOND, 0);

                    // If it's after the alarm time, schedule starting alarm for the next week
                    if ( curr_cal.getTimeInMillis() > cal.getTimeInMillis()) {
                        cal.add(Calendar.DAY_OF_YEAR, 7); // add, not set!

                        // Check if any of the following alarms for this active period are not past
                        for(int t = 1; t < 4; t++) {
                            // Get alarm time
                            Calendar cal2 = Calendar.getInstance();
                            cal2.set(Calendar.DAY_OF_WEEK, currDay);
                            cal2.set(Calendar.HOUR_OF_DAY, hr);
                            cal2.set(Calendar.MINUTE, min + t*(duration/4));
                            cal2.set(Calendar.SECOND, 0);

                            // If there is an iteration that isn't past the current time, schedule alarm
                            if ( curr_cal.getTimeInMillis() < cal2.getTimeInMillis()) {
                                // Alarm iteration
                                int iter = t + 1;

                                // Parial ID for the pending intent
                                String partID = String.valueOf(survey_id) + // survey id
                                        String.valueOf(d) +                 // day
                                        String.valueOf(j);                  // time

                                // Full pending intent ID -- partial ID + iteration number
                                int intentID = Integer.parseInt(partID + String.valueOf(iter));

                                Intent notifIntent;
                                notifIntent = new Intent(context, NotificationService.class);
                                notifIntent.putExtra("ID", survey_id);
                                notifIntent.putExtra("PART_ID", partID);
                                notifIntent.putExtra("ITER", iter);
                                notifIntent.putExtra("TIME", String.valueOf(hr) + ":" + String.valueOf(min));

                                PendingIntent notifPendingIntent2 = PendingIntent.getService(
                                        context,
                                        intentID,
                                        notifIntent,
                                        PendingIntent.FLAG_CANCEL_CURRENT);

                                // Set alarm for survey notification
                                alarmManager.set(AlarmManager.RTC_WAKEUP, cal2.getTimeInMillis(), notifPendingIntent2);

                                break;
                            }
                        }
                    }

                    // Partial ID for the pending intent
                    String partID = String.valueOf(d) + String.valueOf(survey_id) + String.valueOf(j);

                    // Full pending intent ID -- partial ID + iteration number
                    int intentID  = Integer.parseInt(partID + "1");

                    Intent notifIntent = new Intent(context, NotificationService.class);
                    notifIntent.putExtra("ID", survey_id);
                    notifIntent.putExtra("PART_ID", partID);
                    notifIntent.putExtra("ITER", 1);
                    notifIntent.putExtra("TIME", String.valueOf(hr) + ":" + String.valueOf(min));

                    PendingIntent notifPendingIntent = PendingIntent.getService(
                            context,
                            intentID,
                            notifIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT);

                    // Set alarm for survey notification
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            cal.getTimeInMillis(),
                            notifPendingIntent);
                }
            }
        }
    }

    public void start_UpdatesService(Context context) {
        /**
         * This function sets an alarm for the update service
        **/

        Intent serviceIntent = new Intent(context, UpdateService.class);
        serviceIntent.putExtra("FROM_BOOT", true); // Let the service know that it was called from the OnBoot class

        PendingIntent pendingIntent = PendingIntent.getService(
                context,
                18,
                serviceIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        // Get random time between 12 AM and 4 AM
        Random rand = new Random();

        int randomHr  = rand.nextInt((4) + 1);
        int randomMin = rand.nextInt((59) + 1);
        int randomSec = rand.nextInt((59) + 1);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, randomHr);
        cal.set(Calendar.MINUTE, randomMin);
        cal.set(Calendar.SECOND, randomSec);

        Calendar curr_cal = Calendar.getInstance();

        // If it's after the alarm time, schedule for next day
        if ( curr_cal.getTimeInMillis() > cal.getTimeInMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1); // add, not set!
        }

        alarmManager.setRepeating(
                AlarmManager.RTC,
                cal.getTimeInMillis(),
                alarmManager.INTERVAL_DAY,
                pendingIntent);
    }
}
