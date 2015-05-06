package com.example.natalie.android_wellbeing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;

/**
 * Created by Natalie on 12/17/2014.
**/

public class ReminderDialog extends Activity {
    /** This active creates a user dialog popup as the last reminder for the active period. The user
     *  is able to either dismiss the reminder or go to the survey
    **/

    private int ID = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get survey ID from the caller intent
        Intent caller = getIntent();
        ID = caller.getIntExtra("ID", 1);

        // Initialize SQLite database and the dialog builder
        final SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // Set title for the dialog
        String title = dbHandler.getName(ID);
        alertDialogBuilder.setTitle(title + " Survey");

        // Set dialog message
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
                        Intent i = new Intent(getApplicationContext(), Checkpoint.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        i.putExtra("ID", ID);

                        startService(i);
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
