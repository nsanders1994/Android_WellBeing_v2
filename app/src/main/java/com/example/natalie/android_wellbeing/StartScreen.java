package com.example.natalie.android_wellbeing;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.media.Image;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class StartScreen extends Activity implements UpdateResultReceiver.Receiver{
    SurveyDatabaseHandler dbHandler;
    List<Integer> survey_ids = new ArrayList<>();
    ListView startListView;
    StartListAdapter startListAdapter;
    int surveysImported = 0;

    @Override
    protected void onResume() {
        super.onResume();
        startListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case UpdateService.STATUS_RUNNING:
                setProgressBarIndeterminateVisibility(true);
                break;

            case UpdateService.STATUS_FINISHED:
                // Hide progress & extract result from bundle
                int ct = resultData.getInt("SURVEYCT", 0);
                surveysImported++;

                if(ct == surveysImported){
                    setProgressBarIndeterminateVisibility(false);

                    // Update ListView with result
                    survey_ids = new ArrayList<>();
                    startListAdapter.notifyDataSetInvalidated();
                    startListAdapter.notifyDataSetChanged();
                }

                break;

            case UpdateService.STATUS_ERROR:
                /* Handle the error */
                String error = resultData.getString(Intent.EXTRA_TEXT);
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                break;

            case OnBoot.STATUS_BOOT:
                Log.i("UPDATE SERVICE>>>", "Set Update alarm on boot...");

                // Start Update Service
                UpdateResultReceiver mReceiver = new UpdateResultReceiver(new Handler());
                mReceiver.setReceiver(this);
                Intent updateIntent = new Intent(Intent.ACTION_SYNC, null, this, UpdateService.class);

                // Send optional extras to Download IntentService
                updateIntent.putExtra("receiver", mReceiver);
                updateIntent.putExtra("requestId", 101);

                start_UpdatesService(updateIntent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);

        // Check for Toast
        Intent caller = getIntent();
        boolean toast = caller.getBooleanExtra("TOAST", false);

        if(toast){
            Toast.makeText(getApplicationContext(), "The survey requested is inactive.", Toast.LENGTH_SHORT).show();
        }

        // Start Update Service
        UpdateResultReceiver mReceiver = new UpdateResultReceiver(new Handler());
        mReceiver.setReceiver(this);
        Intent updateIntent = new Intent(Intent.ACTION_SYNC, null, this, UpdateService.class);

        // Send optional extras to Download IntentService
        updateIntent.putExtra("receiver", mReceiver);
        updateIntent.putExtra("requestId", 101);

        start_UpdatesService(updateIntent);
        Log.i("UPDATE SERVICE>>>", "Set alarm for update service");
        // Fill ListView
        dbHandler        = new SurveyDatabaseHandler(getApplicationContext());
        startListView    = (ListView) findViewById(R.id.listView);
        startListAdapter = new StartListAdapter();
        startListView.setAdapter(startListAdapter);

        // If app was just installed get all surveys from Parse and store in database
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean emailStored = prefs.getBoolean(getString(R.string.emailStored), false);

        if(!emailStored) {
            Intent intent = new Intent(StartScreen.this, EmailDialog.class);
            startActivity(intent);
        }

        startListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                //checkForUpdate();

                if(survey_ids.size() == 0) {
                    survey_ids = dbHandler.getSurveyIDs();
                }

                Log.i("DEBUG>>>>", "# of surveys = " + String.valueOf(survey_ids.size()) );
                int curr_id = survey_ids.get(position);

                if(Utilities.validTime(getApplicationContext(), curr_id)){
                    Intent intent = new Intent(StartScreen.this, SurveyScreen.class);
                    intent.putExtra("ID", survey_ids.get(position));
                    startActivity(intent);
                }
            }
        });
    }

    public void start_UpdatesService(Intent intent) {

        PendingIntent pendingIntent = PendingIntent.getService(
                getApplicationContext(),
                18,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        // Get random time
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomHr  = 3;//rand.nextInt((4) + 1);
        int randomMin = 23;//rand.nextInt((59) + 1);
        int randomSec = 0;//rand.nextInt((59) + 1);

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

    public class StartListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return dbHandler.getSurveyCount();
        }

        @Override
        public Object getItem(int arg0) {
            return null;
        }

        @Override
        public long getItemId(int arg0) {
            return arg0;
        }

        @Override
        public View getView(int arg0, View arg1, ViewGroup arg2) {
            Log.i("SYNC>>>", "in getView, surveyCt = " + String.valueOf(dbHandler.getSurveyCount()));
            Log.i("SYNC>>>", "in getView, ids = " + String.valueOf(dbHandler.getSurveyIDs()));
            if(arg1==null)
            {
                LayoutInflater inflater = (LayoutInflater) StartScreen.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                arg1 = inflater.inflate(R.layout.list_item, arg2,false);
            }

            TextView name  = (TextView)arg1.findViewById(R.id.txtName);
            TextView time  = (TextView)arg1.findViewById(R.id.txtTime);
            ImageView pic = (ImageView)arg1.findViewById(R.id.imageView);

            List<String> times = dbHandler.getTimes(arg0 + 1);
            List<Integer> days = dbHandler.getDays(arg0 + 1);
            int timeCt = times.size();
            int dayCt  = days.size();
            String timeStr = "";
            String dayStr  = "";

            for(int d = 0; d < dayCt; d++) {
                // Create day string
                int currDay = days.get(d) + 1;
                switch(currDay){
                    case 1:
                        dayStr += "Su";
                        break;
                    case 2:
                        dayStr += "M";
                        break;
                    case 3:
                        dayStr += "T";
                        break;
                    case 4:
                        dayStr += "W";
                        break;
                    case 5:
                        dayStr += "R";
                        break;
                    case 6:
                        dayStr += "F";
                        break;
                    case 7:
                        dayStr += "S";
                        break;
                }
            }

            // Create time string
            for(int j = 0; j < timeCt; j++){
                // Calculate survey start time
                int milhr0 = Integer.parseInt(times.get(j).split(":")[0]);
                int min0 = Integer.parseInt(times.get(j).split(":")[1]);

                Calendar calendar0 = Calendar.getInstance();
                calendar0.set(Calendar.HOUR_OF_DAY, milhr0);
                calendar0.set(Calendar.MINUTE, min0);

                int hr0      = calendar0.get(Calendar.HOUR) == 0 ? 12 : calendar0.get(Calendar.HOUR);
                String zone0 = (calendar0.get(Calendar.AM_PM) == Calendar.AM) ? "AM" : "PM";
                String t0    = String.valueOf(hr0) + ":" +
                               ("00" + min0).substring(String.valueOf(min0).length()) + " " +
                               zone0;
                Log.i("SYNC>>>", "arg0 + 1 = " + String.valueOf(arg0+1));
                // Calculate survey closing time
                int duration = dbHandler.getDuration(arg0 + 1);

                Calendar calendar1 = Calendar.getInstance();
                calendar1.set(Calendar.HOUR_OF_DAY, milhr0);
                calendar1.set(Calendar.MINUTE, min0 + duration);

                int hr1      = calendar1.get(Calendar.HOUR) == 0 ? 12 : calendar1.get(Calendar.HOUR);
                int min1     = calendar1.get(Calendar.MINUTE);
                String zone1 = (calendar1.get(Calendar.AM_PM) == Calendar.AM) ? "AM" : "PM";
                String t1    = String.valueOf(hr1) + ":" +
                               ("00" + min1).substring(String.valueOf(min1).length()) + " " +
                               zone1;

                // Add to time string
                if(j == 0) timeStr = t0 + "-" + t1;
                else timeStr = timeStr + ", " + t0 + "-" + t1;
            }

            // Set strings;
            String name_str = dbHandler.getName(arg0 + 1);
            time.setText(dayStr + " " + timeStr);
            name.setText(name_str);

            // Set pictures
            if(name_str.equals("Spirituality")){
                pic.setImageResource(R.drawable.spirituality);
            }
            else if(name_str.equals("Life")) {
                pic.setImageResource(R.drawable.life);
            }
            else if(name_str.equals("Mood")) {
                pic.setImageResource(R.drawable.mood);
            }
            else if(name_str.equals("Sleep Pattern")) {
                pic.setImageResource(R.drawable.sleep);
            }
            else if(name_str.equals("Social Interaction")) {
                pic.setImageResource(R.drawable.social);
            }
            else if(name_str.equals("Day Reconstruction")) {
                pic.setImageResource(R.drawable.day_reconstructor);
            }
            else if(name_str.equals("Sleep")){
                pic.setImageResource(R.drawable.sleep);
            }
            else {
                pic.setImageResource(R.drawable.app);
            }


            // Color code available vs unavailable surveys
            if(Utilities.validTime(getApplicationContext(), arg0 + 1)){
                time.setTextColor(getResources().getColor(android.R.color.black));
                name.setTextColor(getResources().getColor(android.R.color.black));
            }
            else {
                time.setTextColor(getResources().getColor(android.R.color.darker_gray));
                name.setTextColor(getResources().getColor(android.R.color.darker_gray));
            }

            return arg1;
        }
    }

    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

}


