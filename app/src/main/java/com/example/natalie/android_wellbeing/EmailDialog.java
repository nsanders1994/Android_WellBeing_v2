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
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.regex.Pattern;

/**
 * Created by Natalie on 4/11/2015.
 */
public class EmailDialog extends Activity {
    /* The user is asked once to enter their email for administrative purposes */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Dialog
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // Set up the edit text input
        final EditText input = new EditText(this);

        // Specify the type of input as email
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        alertDialogBuilder.setView(input);

        // Set the dialog message
        alertDialogBuilder
                .setMessage("\nEnter your email address:\n\n")
                .setNeutralButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // TODO Auto-generated method stub
                    }
                })
                .setCancelable(false);

        // Create alert dialog
        final AlertDialog alertDialog = alertDialogBuilder.create();

        // Show alert dialog
        alertDialog.show();

        // Specify alert dialog buttons and their actions
        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String email = input.getText().toString(); // get user input string

                // Standard email pattern as a regex
                Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
                        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                                "\\@" +
                                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                                "(" +
                                "\\." +
                                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                                ")+"
                );

                // If there's no user input, inform user
                if (input.getText().toString().trim().length() == 0) {
                    alertDialog.setMessage("You must enter an email address\n\n");

                // If the input string does not follow the standard email regex, inform user
                } else if (!EMAIL_ADDRESS_PATTERN.matcher(email).matches()) {
                    alertDialog.setMessage("Invalid email address\n\n");

                // Otherwise, the input email is valid and is stored in shared preferences
                } else {
                    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                    SharedPreferences.Editor edit = prefs.edit();
                    edit.putBoolean(getString(R.string.emailStored), Boolean.TRUE);
                    edit.putString(getString(R.string.user_email), email);
                    edit.commit();

                    alertDialog.cancel();
                    finish();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Disable the back button, forcing the user to input an email
        Log.i("DEBUG>>>", "Back not allowed");
    }
}
