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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bolts.Task;

public class FinishScreen extends Activity {
    boolean               back_valid = true;
    int                   ans_ct = 0;
    List<Integer>         ans;
    List<Long>            tstamp;
    int                   quesCt;
    int                   id;
    int                   version;
    SurveyDatabaseHandler dbHandler;
    AlarmManager          alarmManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHandler = new SurveyDatabaseHandler(getApplicationContext());
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        setContentView(R.layout.activity_finish_screen);

        final Button submitBttn = (Button) findViewById(R.id.bttnNext);
        final Button prevBttn   = (Button) findViewById(R.id.bttnBack);
        TextView progressTxt    = (TextView) findViewById(R.id.txtProgress);

        Intent curr_intent = getIntent();
        ans_ct   = curr_intent.getIntExtra("CT", 0);
        id       = curr_intent.getIntExtra("ID", 0);

        ans = dbHandler.getUserAns(id);
        tstamp = dbHandler.getTStamps(id);
        version  = dbHandler.getVersion(id);
        quesCt     = ans.size();

        progressTxt.setText(ans_ct + "/" + quesCt + " Questions Answered");

        submitBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "The survey has been submitted.", Toast.LENGTH_SHORT).show();
                back_valid = false;

                // Submit survey to parse website
                sendToParse();

                // Set survey to completed
                dbHandler.setComplete(true, id);

                // Clear answer list
                dbHandler.storeAnswers("empty", id);
                dbHandler.storeTStamps("empty", id);

                // Return to home screen
                Intent intent = new Intent(FinishScreen.this, StartScreen.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });

        prevBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
        String app_version = getResources().getString(R.string.app_version);

        for(int i = 0; i < quesCt; i++) {
            ParseObject ques = new ParseObject("AllSurveyResponses");

            ques.put("appID", app_version);
            ques.put("userEmail", email);
            ques.put("userID", InstallationID.id(this));
            ques.put("surveyID", version);
            ques.put("questionID", i+1);
            ques.put("questionResponse", ans.get(i));
            ques.put("unixTimeStamp", tstamp.get(i));

            ques.saveInBackground();

            if(i%25 == 0){
                try {
                    Thread.sleep(500);                 //1000 milliseconds is one second.
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}