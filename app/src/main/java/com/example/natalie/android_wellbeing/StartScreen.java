package com.example.natalie.android_wellbeing;

import android.media.Image;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;


public class StartScreen extends Activity {
    SurveyDatabaseHandler dbHandler;
    List<Integer> survey_ids = new ArrayList<Integer>();
    ListView startListView;
    StartListAdapter startListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHandler = new SurveyDatabaseHandler(getApplicationContext());

        // If app was just installed get all surveys from Parse and store in database
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean emailStored = prefs.getBoolean(getString(R.string.emailStored), false);

        if(!emailStored) {
            Intent intent = new Intent(StartScreen.this, EmailDialog.class);
            startActivity(intent);
        }


        // Set start view
        setContentView(R.layout.activity_start_screen);

        startListView    = (ListView) findViewById(R.id.listView);
        startListAdapter = new StartListAdapter();
        startListView.setAdapter(startListAdapter);
        survey_ids = dbHandler.getSurveyIDs();

        // Start background service to check for updated popup times
        start_UpdatesService();

        startListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                if(survey_ids.size() == 0) {
                    survey_ids = dbHandler.getSurveyIDs();
                }

                Log.i("DEBUG>>>>", "# of surveys = " + String.valueOf(survey_ids.size()) );
                int curr_id = survey_ids.get(position);

                List<String> times = dbHandler.getTimes(curr_id);
                int timeCt = times.size();

                for(int i = 0; i < timeCt; i++) {
                    // Calculate survey start time
                    Calendar calendar_init0 = Calendar.getInstance();
                    calendar_init0.set(Calendar.HOUR_OF_DAY, 0);
                    calendar_init0.set(Calendar.MINUTE, 0);

                    int militaryHr0 = Integer.parseInt(times.get(i).split(":")[0]);
                    int min0 = Integer.parseInt(times.get(i).split(":")[1]);

                    Calendar calendar0 = calendar_init0;
                    calendar0.add(Calendar.HOUR_OF_DAY, militaryHr0);
                    calendar0.add(Calendar.MINUTE, min0);

                    int hr0 = calendar0.get(Calendar.HOUR) == 0 ? 12 : calendar0.get(Calendar.HOUR);
                    String zone0 = (calendar0.get(Calendar.AM_PM) == Calendar.AM) ? "AM" : "PM";
                    String t0 = String.valueOf(hr0) + ":" +
                            ("00" + min0).substring(String.valueOf(min0).length()) + " " +
                            zone0;

                    // Calculate survey closing time
                    int duration = dbHandler.getDuration(curr_id);

                    Calendar calendar_init1 = Calendar.getInstance();
                    calendar_init1.set(Calendar.HOUR_OF_DAY, 0);
                    calendar_init1.set(Calendar.MINUTE, 0);

                    Calendar calendar1 = calendar_init1;
                    calendar1.add(Calendar.HOUR_OF_DAY, militaryHr0);
                    calendar1.add(Calendar.MINUTE, min0 + duration);

                    // Set clickable/unclickable
                    boolean completed = dbHandler.isCompleted(curr_id);
                    Calendar curr_calendar = Calendar.getInstance();
                    Intent intent;

                    Log.i("DEBUG>>>>> ","Completed = " + (completed ? "True" : "False"));
                    if ( completed ||
                         curr_calendar.getTimeInMillis() < calendar0.getTimeInMillis() ||
                         curr_calendar.getTimeInMillis() > calendar1.getTimeInMillis()) {
                        int hr = curr_calendar.get(Calendar.HOUR);
                        int min = curr_calendar.get(Calendar.MINUTE);
                        Log.i("DEBUG>>>>>","Unavailable at " + String.valueOf(hr) + ":" + String.valueOf(min));
                    }
                    else {
                        intent = new Intent(StartScreen.this, SurveyScreen.class);
                        intent.putExtra("ID", survey_ids.get(position));
                        startActivity(intent);
                    }
                }
            }
        });
    }

    public void start_UpdatesService() {

        PendingIntent pendingIntent = PendingIntent.getService(
                getApplicationContext(),
                1,
                new Intent(getApplicationContext(), UpdateService.class),
                PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);

        alarmManager.setInexactRepeating(
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
            if(arg1==null)
            {
                LayoutInflater inflater = (LayoutInflater) StartScreen.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                arg1 = inflater.inflate(R.layout.list_item, arg2,false);
            }

            TextView name  = (TextView)arg1.findViewById(R.id.txtName);
            TextView time  = (TextView)arg1.findViewById(R.id.txtTime);
            ImageView pic = (ImageView)arg1.findViewById(R.id.imageView);



            List<String> times = dbHandler.getTimes(arg0 + 1);
            int timeCt = times.size();
            boolean accessible = false;
            String t_string = "";

            for(int j = 0; j < timeCt; j++){
                // Calculate survey start time
                Calendar calendar_init0 = Calendar.getInstance();
                calendar_init0.set(Calendar.HOUR_OF_DAY, 0);
                calendar_init0.set(Calendar.MINUTE, 0);

                int militaryHr0 = Integer.parseInt(times.get(j).split(":")[0]);
                int min0 = Integer.parseInt(times.get(j).split(":")[1]);

                Calendar calendar0 = calendar_init0;
                calendar0.add(Calendar.HOUR_OF_DAY, militaryHr0);
                calendar0.add(Calendar.MINUTE, min0);

                int hr0 = calendar0.get(Calendar.HOUR) == 0 ? 12 : calendar0.get(Calendar.HOUR);
                String zone0 = (calendar0.get(Calendar.AM_PM) == Calendar.AM) ? "AM" : "PM";
                String t0 = String.valueOf(hr0) + ":" +
                        ("00" + min0).substring(String.valueOf(min0).length()) + " " +
                        zone0;

                // Calculate survey closing time
                int duration = dbHandler.getDuration(arg0 + 1);

                Calendar calendar_init1 = Calendar.getInstance();
                calendar_init1.set(Calendar.HOUR_OF_DAY, 0);
                calendar_init1.set(Calendar.MINUTE, 0);

                Calendar calendar1 = calendar_init1;
                calendar1.add(Calendar.HOUR_OF_DAY, militaryHr0);
                calendar1.add(Calendar.MINUTE, min0 + duration);

                int hr1 = calendar1.get(Calendar.HOUR) == 0 ? 12 : calendar1.get(Calendar.HOUR);
                int min1 = calendar1.get(Calendar.MINUTE);
                String zone1 = (calendar1.get(Calendar.AM_PM) == Calendar.AM) ? "AM" : "PM";
                String t1 = String.valueOf(hr1) + ":" +
                        ("00" + min1).substring(String.valueOf(min1).length()) + " " +
                        zone1;

                // Add to time string
                if(j == 0) {
                    t_string = t0 + "-" + t1;
                }
                else {
                    t_string = t_string + ", " + t0 + "-" + t1;
                }

                // Check if survey is available
                Calendar curr_calendar = Calendar.getInstance();

                if ( curr_calendar.getTimeInMillis() > calendar0.getTimeInMillis() &&
                     curr_calendar.getTimeInMillis() < calendar1.getTimeInMillis()) {
                    accessible = true;
                }
            }

            // Set strings;
            String name_str = dbHandler.getName(arg0 + 1);
            time.setText(t_string);
            name.setText(name_str);

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

            // Gray out text if not available
            boolean completed = dbHandler.isCompleted(arg0 + 1);
            if(!accessible || completed) {
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


