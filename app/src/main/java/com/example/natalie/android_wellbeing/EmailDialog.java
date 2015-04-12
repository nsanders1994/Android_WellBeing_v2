package com.example.natalie.android_wellbeing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by Natalie on 4/11/2015.
 */
public class EmailDialog extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Dialog
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // Set title
        //alertDialogBuilder.setTitle("Email Address");

        // Set up the input
        final EditText input = new EditText(this);

        // Specify the type of input as email
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        alertDialogBuilder.setView(input);

        // set dialog message
        alertDialogBuilder
                .setMessage("\nPlease, enter your email address:\n")
                .setNeutralButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String email = input.getText().toString();

                        if(email.matches("")) {
                            alertDialogBuilder.setMessage("You must enter an email address:");
                        }
                        else {
                            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                            SharedPreferences.Editor edit = prefs.edit();
                            edit.putBoolean(getString(R.string.emailStored), Boolean.TRUE);
                            edit.putString(getString(R.string.user_email), email);
                            edit.commit();

                            dialogInterface.cancel();
                            finish();
                        }
                    }
                });


        // Create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // Show alert dialog
        alertDialog.show();
    }
}
