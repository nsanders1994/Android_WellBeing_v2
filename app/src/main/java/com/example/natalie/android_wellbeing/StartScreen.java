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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class StartScreen extends Activity implements UpdateResultReceiver.Receiver{
    /**
     * This activity displays the main menu of the app-- a ListView displaying all available surveys,
     * their respective days/times, and their icon. All active surveys are listed in black while
     * inactive surveys are listed in grey. Clicking on an active survey listing will bring the user
     * to the survey itself
    **/

    private SurveyDatabaseHandler dbHandler;                // handler for the SQLite database
    private List<Integer> survey_ids = new ArrayList<>();   // list of the IDs for all surveys
    private ListView startListView;                         // the ListView widget
    private StartListAdapter startListAdapter;              // the ListView adapter which populates the view
    private int surveysImported = 0;                        // the number of surveys imported

    @Override
    protected void onResume() {
        super.onResume();
        // Repopulate the ListView in case an active period has started/expired and the color coding
        // of the listings needs to be changed
        startListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        /**
         * This broadcast receiver receives updates from the update service on how many surveys have
         * been imported so far and how many total need to be imported. Once all surveys are imported
         * the ListView is repopulated to reflect the new data.
        **/

        switch (resultCode) {
            case UpdateService.STATUS_RUNNING:
                // Alerts the start view that the update service has stated
                setProgressBarIndeterminateVisibility(true);
                break;

            case UpdateService.STATUS_FINISHED:
                // Alerts the start view that the update service has finished importing a survey

                // Hide progress & extract result from bundle
                int ct = resultData.getInt("SURVEYCT", 0);
                surveysImported++;

                // If all surveys have been imported, update the ListView
                if(ct == surveysImported){
                    Log.i("DEBUG>>", "Repopulating the listview");
                    setProgressBarIndeterminateVisibility(false);

                    // Update ListView with result
                    survey_ids = new ArrayList<>();
                    startListAdapter.notifyDataSetInvalidated();
                    startListAdapter.notifyDataSetChanged();
                }

                break;

            case UpdateService.STATUS_ERROR:
                // An error occured while trying to import a survey

                String error = resultData.getString(Intent.EXTRA_TEXT);
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);

        // If a user clicks a notification/dialog for a survey that has since inspired, they are taken
        // here to the start view with the TOAST flag set. If set, a Toast message appears notifying the
        // user that the survey they requested has expired
        Intent caller = getIntent();
        boolean toast = caller.getBooleanExtra("TOAST", false);

        if(toast){
            Toast.makeText(getApplicationContext(), "The survey requested is inactive.", Toast.LENGTH_SHORT).show();
        }

        // Set alarm for the Update Service and pass connection to the receiver
        UpdateResultReceiver mReceiver = new UpdateResultReceiver(new Handler());
        mReceiver.setReceiver(this);
        Intent updateIntent = new Intent(Intent.ACTION_SYNC, null, this, UpdateService.class);

        updateIntent.putExtra("receiver", mReceiver);
        updateIntent.putExtra("requestId", 101);

        start_UpdatesService(updateIntent);
        Log.i("UPDATE SERVICE>>>", "Set alarm for update service");

        // Initialize the SQLite database handler, ListView, and ListView adapter
        dbHandler        = new SurveyDatabaseHandler(getApplicationContext());
        startListView    = (ListView) findViewById(R.id.listView);
        startListAdapter = new StartListAdapter();

        // Populate the ListView
        startListView.setAdapter(startListAdapter);

        // If no email is stored for the user, start the EmailDialog activity to get the user's email
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean emailStored = prefs.getBoolean(getString(R.string.emailStored), false);

        if(!emailStored) {
            Intent intent = new Intent(StartScreen.this, EmailDialog.class);
            startActivity(intent);
        }

        // List for a user click on the ListView
        startListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // Get IDs for all surveys
                if(survey_ids.size() == 0) {
                    survey_ids = dbHandler.getSurveyIDs();
                }

                // Get ID of the survey the user selected
                int curr_id = survey_ids.get(position);

                // If the survey is active, take the user to the survey
                if(Utilities.surveyOpen(getApplicationContext(), curr_id)){
                    Intent intent = new Intent(StartScreen.this, SurveyScreen.class);
                    intent.putExtra("ID", survey_ids.get(position));
                    startActivity(intent);
                }
            }
        });
    }

    public void start_UpdatesService(Intent intent) {
        /**
         * This function sets an alarm for the update service
         **/

        PendingIntent pendingIntent = PendingIntent.getService(
                getApplicationContext(),
                18,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        // Get random time between 12 am and 4 am
        Random rand = new Random();

        int randomHr  = rand.nextInt((4) + 1);
        int randomMin = rand.nextInt((59) + 1);
        int randomSec = rand.nextInt((59) + 1);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, randomHr);
        cal.set(Calendar.MINUTE, randomMin);
        cal.set(Calendar.SECOND, randomSec);

        Calendar curr_cal = Calendar.getInstance();

        // If it's after the alarm time, schedule for next day
        if ( curr_cal.getTimeInMillis() > cal.getTimeInMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1); // add, not set!
        }

        // Set alarm
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
            /**
             *  Adds the listing for the specified survey
            **/

            // Inflate layout if the ListView is empty
            if(arg1==null)
            {
                LayoutInflater inflater = (LayoutInflater) StartScreen.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                arg1 = inflater.inflate(R.layout.list_item, arg2,false);
            }

            TextView name  = (TextView)arg1.findViewById(R.id.txtName);     // survey name text
            TextView time  = (TextView)arg1.findViewById(R.id.txtTime);     // survey time/s text
            ImageView pic = (ImageView)arg1.findViewById(R.id.imageView);   // survey icon

            List<String> times = dbHandler.getTimes(arg0 + 1);              // all active period start times
            List<Integer> days = dbHandler.getDays(arg0 + 1);               // all days with active periods
            int timeCt = times.size();                                      // number of active periods
            int dayCt  = days.size();                                       // number of active days
            String timeStr = "";                                            // time/s string
            String dayStr  = "";                                            // day/s string

            // Create a string listing all days the survey is active
            for(int d = 0; d < dayCt; d++) {
                int currDay = days.get(d) + 1; // 1 is added because provided values are 0-6, Android uses 1-7
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

            // Create a string listing all the active period times
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

            // Set text views;
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

            // Color code available vs. unavailable surveys
            if(Utilities.surveyOpen(getApplicationContext(), arg0 + 1)){
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
        /**
         * When the user presses the device's back key always take them back to the device's home screen
        **/

        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

}


