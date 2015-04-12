package com.example.natalie.android_wellbeing;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.widget.Toast;

/**
 * Created by Natalie on 12/17/2014.
 */
public class ReminderDialog extends Activity {
    int ID = 0;
    int iteration = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get survey ID from caller intent
        Intent caller = getIntent();
        ID = caller.getIntExtra("ID", 1);
        iteration = caller.getIntExtra("ITERATION", 0);

        // Initialize Database
        final SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        dbHandler.setComplete(false, ID);

        // Set title
        String title = dbHandler.getName(ID);
        alertDialogBuilder.setTitle(title + " Survey");

        // set dialog message
        alertDialogBuilder
                .setMessage("\nAre you ready to take your survey?\n")
                .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                        finish();
                    }
                })
                .setNeutralButton("Okay", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent i = new Intent(getApplicationContext(), SurveyScreen.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        i.putExtra("ID", ID);

                        startActivity(i);
                        dialog.cancel();
                        finish();

                    }
                });


        // Create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // Show alert dialog
        alertDialog.show();

        // Vibrate phone
        Vibrator vibrator;
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(500);

        // Sound alarm
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
