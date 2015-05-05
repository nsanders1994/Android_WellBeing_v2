package com.example.natalie.android_wellbeing;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Created by Natalie on 12/17/2014.
 */
public class OnBoot extends BroadcastReceiver {
    public static final int STATUS_BOOT = 3;

    @Override
    public void onReceive(Context context, Intent intent) {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        start_UpdatesService(context);

        // Retrieve number of surveys from database
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(context);
        List<Integer> survey_ids = dbHandler.getSurveyIDs();

        // Create a repeating alarm for popup for every survey at the specified time
        for (int k = 0; k < survey_ids.size(); k++) {

            int survey_id = survey_ids.get(k);
            List<String> times = dbHandler.getTimes(survey_id);
            List<Integer> days = dbHandler.getDays(survey_id);
            int dayCt = days.size();
            int timeCt = times.size();
            int duration = dbHandler.getDuration(survey_id);

            for(int d = 0; d < dayCt; d++) {
                int currDay = Integer.parseInt(String.valueOf(days.get(d))) + 1; // days are give 0-6, android uses 1-7
                // Set first alarm for all survey times
                for(int j = 0; j < timeCt; j++) {
                    int hr  = Integer.parseInt(String.valueOf(times.get(j)).split(":")[0]);
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
                    if ( curr_cal.getTimeInMillis() > cal.getTimeInMillis()) {
                        Log.i("TIME>>>", "\tSurvey alarm scheduled for next week");
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
                                int iter = t + 1;
                                String partID = String.valueOf(survey_id) + // survey id
                                        String.valueOf(d) +         // day
                                        String.valueOf(j);          // time

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

                                Log.i("TIME>>>", "Secondary Alarm for " + String.valueOf(survey_id) + " = " + cal2.getTime().toString());
                                // Set alarm for survey notification
                                alarmManager.set(AlarmManager.RTC_WAKEUP, cal2.getTimeInMillis(), notifPendingIntent2);

                                break;
                            }
                        }
                    }

                    Log.i("TIME>>>", "Primary Alarm for " + String.valueOf(survey_id) + " = " + cal.getTime().toString());

                    String partID = String.valueOf(d) + String.valueOf(survey_id) + String.valueOf(j);
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
                    alarmManager.set/*Repeating*/(
                            AlarmManager.RTC_WAKEUP,
                            cal.getTimeInMillis(),
                            //7*alarmManager.INTERVAL_DAY,
                            notifPendingIntent);
                }
            }
        }
    }

    public void start_UpdatesService(Context context) {
        Intent serviceIntent = new Intent(context, UpdateService.class);
        serviceIntent.putExtra("FROM_BOOT", true);

        PendingIntent pendingIntent = PendingIntent.getService(
                context,
                18,
                serviceIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        // Get random time
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomHr  = rand.nextInt((4) + 1);
        int randomMin = rand.nextInt((59) + 1);
        int randomSec = rand.nextInt((59) + 1);

        Log.i("RANDOM>>>", String.valueOf(randomHr) + ":" + String.valueOf(randomMin) + ":" + String.valueOf(randomSec));

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
