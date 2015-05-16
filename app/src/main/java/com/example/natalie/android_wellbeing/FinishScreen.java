package com.example.natalie.android_wellbeing;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.app.Activity;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.parse.ParseObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class FinishScreen extends Activity {
    /**
     * This activity produces the end screen of the survey, displayed after the user finishes the
     * last question. It allows the user to submit the survey to Parse or go back to the last question.
    **/

    int                   ans_ct = 0;   // the number of questions answered
    List<String>          ans;          // list of the user's answers
    List<Long>            tstamp;       // list of the timestamps for when the user answered
    int                   quesCt;       // the number of questions in the survey
    int                   id;           // the current survey's ID
    float                 version;      // the current survey's version number
    SurveyDatabaseHandler dbHandler;    // handler for the SQLite database
    long                  lastTouch;    // keeps track of when the user last interacted with the app

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHandler = new SurveyDatabaseHandler(getApplicationContext());
        setContentView(R.layout.activity_finish_screen);

        // Initialize lastTouch variable
        lastTouch = Calendar.getInstance().getTimeInMillis();

        // Initialize layout widgets
        final Button submitBttn = (Button) findViewById(R.id.bttnNext);
        final Button prevBttn   = (Button) findViewById(R.id.bttnBack);
        TextView progressTxt    = (TextView) findViewById(R.id.txtProgress);

        // Get number of questions answered and the survey's ID from the previous activity
        Intent curr_intent = getIntent();
        ans_ct   = curr_intent.getIntExtra("CT", 0);
        id       = curr_intent.getIntExtra("ID", 0);

        // Get the user's answers, timestamps, survey version, and number of questions from the database
        ans     = dbHandler.getUserAns(id);
        tstamp  = dbHandler.getTStamps(id);
        version = dbHandler.getVersion(id);
        Log.i("DEBUG>>", "Version # in Finish = " + String.valueOf(version));
        quesCt  = ans.size();

        // Display user's progress on the survey
        progressTxt.setText(ans_ct + "/" + quesCt + " Questions Answered");

        // Listen for when the submit button is clicked
        submitBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // If the survey is still active...
                if(checkSurveyActive()) {
                    // Check that the app has an email for the user
                    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                    boolean emailStored = prefs.getBoolean(getString(R.string.emailStored), false);

                    if(!emailStored) {
                        Intent intent = new Intent(FinishScreen.this, EmailDialog.class);
                        startActivity(intent);
                    }

                    ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                    if (!mWifi.isConnected()) {
                        Toast.makeText(getApplicationContext(),
                                "You have no WiFi! Could not submit survey.Your answers have been saved.",
                                Toast.LENGTH_SHORT).show();

                        //Return to the home screen
                        Intent intent = new Intent(FinishScreen.this, StartScreen.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                    else{
                        // Notify the user that the survey is submitted
                        Toast.makeText(getApplicationContext(), "The survey has been submitted.", Toast.LENGTH_SHORT).show();

                        // Submit survey to parse website
                        sendToParse();

                        // Set survey to completed
                        dbHandler.setComplete(true, id);

                        // Clear answer and timestamp lists
                        dbHandler.storeAnswers("empty", id);
                        dbHandler.storeTStamps("empty", id);

                        // Return to home screen
                        Intent intent = new Intent(FinishScreen.this, StartScreen.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                }
                else{
                    // Notify the user that the survey is no longer active
                    Toast.makeText(getApplicationContext(), "Survey no longer active.", Toast.LENGTH_SHORT).show();

                    // Clear answer and timestamp lists
                    dbHandler.storeAnswers("empty", id);
                    dbHandler.storeTStamps("empty", id);

                    //Return to the home screen
                    Intent intent = new Intent(FinishScreen.this, StartScreen.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
            }
        });

        // Listen for when the back button is clicked
        prevBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // If the survey is still active...
                if(checkSurveyActive()){
                    Intent intent = new Intent(FinishScreen.this, SurveyScreen.class);

                    // Convert the answer and timestamp lists to object lists in order to join by delimiter
                    List<Object> temp1 = new ArrayList<Object>();
                    List<Object> temp2 = new ArrayList<Object>();

                    temp1.addAll(ans);
                    temp2.addAll(tstamp);

                    // Store answers and timestamps in the database
                    dbHandler.storeAnswers(Utilities.join(temp1, "`nxt`"), id);
                    dbHandler.storeTStamps(Utilities.join(temp2, ","), id);
                    intent.putExtra("ID", id);

                    // Return to the previous activity (SurveyScreen)
                    setResult(3, intent);
                    finish();
                }
                else{
                    // Notify the user that the survey is no longer active
                    Toast.makeText(getApplicationContext(), "Survey no longer active.", Toast.LENGTH_SHORT).show();

                    // Clear the answer and timestamp lists
                    dbHandler.storeAnswers("empty", id);
                    dbHandler.storeTStamps("empty", id);

                    // Return to the home screen
                    Intent intent = new Intent(FinishScreen.this, StartScreen.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        /**
         * Perform same action as if the user had pressed the back button widget
        **/

        // Check that the survey is still active
        checkSurveyActive();

        Intent intent = new Intent(FinishScreen.this, SurveyScreen.class);

        // Convert the answer and timestamp lists to object lists in order to join by delimiter
        List<Object> temp1 = new ArrayList<Object>();
        List<Object> temp2 = new ArrayList<Object>();

        temp1.addAll(ans);
        temp2.addAll(tstamp);

        // Store answers and timestamps in the database
        dbHandler.storeAnswers(Utilities.join(temp1, "`nxt`"), id);
        dbHandler.storeTStamps(Utilities.join(temp2, ","), id);
        intent.putExtra("ID", id);

        // Return to the previous activity (SurveyScreen)
        setResult(3, intent);
        finish();

    }


    public void sendToParse() {
        /**
         * Sends all the survey data and results to be stored in Parse
        **/

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String email            = prefs.getString(getString(R.string.user_email), "ERROR"); // user email
        String app_version      = getResources().getString(R.string.app_version);                   // app version ID
        List<String> ques_types = dbHandler.getQuesTypes(id);             // question types for this survey

        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        String deviceID = telephonyManager.getDeviceId(); // Android IMEI

        // For all questions in the survey...
        for(int i = 0; i < quesCt; i++) {
            List<String> ans_array = new ArrayList<>();
            ParseObject ques = new ParseObject("SurveyResponses");

            ques.put("appID", app_version);              // add the app version ID to the response table
            ques.put("userEmail", email);                // add the user's email to the response table
            ques.put("userID", InstallationID.id(this)); // add the app installation ID to the response table
            ques.put("surveyID", version);               // add the survey version number to the response table
            ques.put("deviceID", deviceID);              // add the Android device ID to the response table
            ques.put("questionID", i+1);                 // add the current question's ID to the response table

            // If the current question is of type "Checkbox", there may be multiple answer values so
            // the answer string must be split by ","
            if(ques_types.get(i).equals("Checkbox")){
                ans_array = Arrays.asList(ans.get(i).split(",")); // put answer/s in an array
            }
            else{
                ans_array.add(ans.get(i));  // put answer in an array
            }

            ques.put("questionResponse", ans_array);    // add answer array to the response table
            ques.put("unixTimeStamp", tstamp.get(i));   // add timestamp for current question to the response table

            // Push to Parse in the background
            ques.saveInBackground();

            // Parse can only handle 30 requests at a time, so wait 0.5 s every 25 saves
            if(i%25 == 0){
                try {
                    Thread.sleep(500);
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public boolean checkSurveyActive(){
        /**
         * Checks if the survey is still active. If it has become inactive, allow the user to continue
         * to fill out the survey as long as the app does not remain idle for more than 5 minutes
         * (The user must be actively filling out the survey to keep it open if it is past its
         * expiration)
        **/

        long curr_time = Calendar.getInstance().getTimeInMillis();  // the current time
        long t_diff = curr_time - lastTouch; // the difference btw the current time and the time the app was last interacted with

        // If the survey is still active...
        if(Utilities.surveyOpen(getApplicationContext(), id)){
            lastTouch = curr_time; // reset the lastTouch time
            return true;           // return true that the survey is still active
        }
        // Otherwise, if the survey is no longer active...
        else{
            // If the difference btw the current time and the lastTouch time is less than 5 minutes...
            if((t_diff/1000)/60 <= 5){
                lastTouch = curr_time; // reset the lastTouch time
                return true;           // return true to keep the survey active and allow the user to finish
            }
            else{
                return false; // Otherwise, the survey is inactive
            }
        }
    }
}