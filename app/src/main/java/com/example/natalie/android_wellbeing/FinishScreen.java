package com.example.natalie.android_wellbeing;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.parse.ParseObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinishScreen extends Activity {
    boolean back_valid    = true;
    //public  Survey survey = new Survey();
    int     ans_ct = 0;
    //int  [] ans;
    //long [] tstamp;
    List<Integer> ans;
    List<Long> tstamp;
    int     size;
    int     id;
    int     set_type;
    int     version;
    SurveyDatabaseHandler dbHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHandler = new SurveyDatabaseHandler(getApplicationContext());

        setContentView(R.layout.activity_finish_screen);

        final Button submitBttn = (Button) findViewById(R.id.bttnNext);
        final Button prevBttn   = (Button) findViewById(R.id.bttnBack);
        TextView progressTxt    = (TextView) findViewById(R.id.txtProgress);

        Intent curr_intent = getIntent();
        ans_ct   = curr_intent.getIntExtra("CT", 0);
        //ans      = curr_intent.getIntArrayExtra("ANS");
        //Log.i("DEBUG>>>>>", "In Finish, ans = " + ans.toString());
        //tstamp   = curr_intent.getLongArrayExtra("TSTAMP");

        id       = curr_intent.getIntExtra("ID", 0);
        ans = dbHandler.getUserAns(id);
        tstamp = dbHandler.getTStamps(id);
        version  = dbHandler.getVersion(id);
        size     = ans.size();


        progressTxt.setText(ans_ct + "/" + size + " Questions Answered");
        //int n = survey.get_num_answered();
        //progressTxt.setText(n + "/4 Questions Answered");

        submitBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "The survey has been submitted.", Toast.LENGTH_SHORT).show();
                back_valid = false;

                // Submit survey to parse website
                sendToParse();

                // Cancel remaining alarms for this time
                int curr_surveyTimeCt = dbHandler.getTimes(id).size();
                for(int k = 0; k < curr_surveyTimeCt; k++){
                    // Cancel all further alarms for this survey time
                    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    Intent alarmIntent = new Intent(getApplicationContext(), PopupService.class);
                    alarmIntent.putExtra("ID", id);
                    alarmIntent.putExtra("ITERATION", k);

                    PendingIntent pendingIntent = PendingIntent.getService(
                            getApplicationContext(),
                            Integer.parseInt(String.valueOf(id) + String.valueOf(k)),
                            alarmIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT);

                    // Cancel alarms
                    try {
                        alarmManager.cancel(pendingIntent);
                    } catch (Exception e) {
                        Log.e("ERROR>>> ", "AlarmManager update was not canceled. " + e.toString());
                    }
                }

                // Set survey to completed
                dbHandler.setComplete(true, id);

                // Clear answer list
                dbHandler.storeAnswers("", id);
                dbHandler.storeTStamps("", id);

                // Return to home screen
                Intent intent = new Intent(FinishScreen.this, StartScreen.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });

        prevBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent;

                if(set_type == 1) {
                    intent = new Intent(FinishScreen.this, SurveyScreen.class);
                }
                else if(set_type == 2) {
                    intent = new Intent(FinishScreen.this, SurveyScreen.class);
                }
                else {
                    intent = new Intent(FinishScreen.this, SurveyScreen.class);
                }

                // Convert to object list in order to join by delimiter
                List<Object> temp1 = new ArrayList<Object>();
                List<Object> temp2 = new ArrayList<Object>();

                temp1.addAll(ans);
                temp2.addAll(tstamp);

                // Store answers and timestamps
                dbHandler.storeAnswers(Utilities.join(temp1, ","), id);
                dbHandler.storeTStamps(Utilities.join(temp2, ","), id);
                intent.putExtra("ID", id);

                setResult(3, intent);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(FinishScreen.this, SurveyScreen.class);

        // Convert to object list in order to join by delimiter
        List<Object> temp1 = new ArrayList<Object>();
        List<Object> temp2 = new ArrayList<Object>();

        temp1.addAll(ans);
        temp2.addAll(tstamp);

        // Store answers and timestamps
        dbHandler.storeAnswers(Utilities.join(temp1, ","), id);
        dbHandler.storeTStamps(Utilities.join(temp2, ","), id);
        intent.putExtra("ID", id);

        setResult(3, intent);
        finish();

    }

    public void sendToParse() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String email = prefs.getString(getString(R.string.user_email), "ERROR");
        for(int i = 0; i < size; i++) {
            ParseObject ques = new ParseObject("SurveyResponseDatabase"); //Installation.id(this));
            ques.put("userId", InstallationID.id(this));
            ques.put("emailId", email);
            ques.put("surveyId", version);
            ques.put("questionId", i+1);
            ques.put("response", ans.get(i));
            ques.put("timestamp", tstamp.get(i));

            ques.saveInBackground();

            /*
            Map<String, String> ques = new HashMap<String, String>();
            ques.put("value", String.valueOf(ans[i]));
            ques.put("timestamp", String.valueOf(tstamp[i]));

            new_survey.put("Q" + String.valueOf(i+1), ques);
            */
        }

    }
}