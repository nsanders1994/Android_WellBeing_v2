package com.example.natalie.android_wellbeing;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;
import java.util.List;

/**
 * Created by Natalie on 12/17/2014.
 */
public class OnBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        // Retrieve number of surveys from database
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(context);
        List<Integer> survey_ids = dbHandler.getSurveyIDs();

        // Create a repeating alarm for popup for every survey at the specified time
        for (int k = 0; k < survey_ids.size(); k++) {

            int survey_id = survey_ids.get(k);
            List<String> times = dbHandler.getQuesTypes(survey_id);
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

                    String timeStr = String.valueOf(cal.get(Calendar.HOUR_OF_DAY)) + ":" + String.valueOf(cal.get(Calendar.MINUTE));

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

                    PendingIntent notifPendingIntent = PendingIntent.getService(
                            context,
                            intentID,
                            notifIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT);

                    // Set alarm for survey notification
                    alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            cal.getTimeInMillis(),
                            7*alarmManager.INTERVAL_DAY,
                            notifPendingIntent);
                }
            }
        }
    }
}
