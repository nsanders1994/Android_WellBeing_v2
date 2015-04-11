package com.example.natalie.android_wellbeing;

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
        boolean previouslyStarted = prefs.getBoolean(getString(R.string.prev_started), false);
        if(!previouslyStarted) {

            final ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("SurveySummary");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    query.findInBackground(new FindCallback<ParseObject>() {
                        public void done(List<ParseObject> all_surveys, ParseException e) {
                            if (e == null) {
                                for (int i = 0; i < all_surveys.size(); i++) {
                                    final int survey_id = i;

                                    ParseObject survey_listing = all_surveys.get(i);
                                    final String name          = survey_listing.getString("SurveyName");
                                    final List<Object> time    = survey_listing.getList("SurveyTime");
                                    final int duration         = survey_listing.getInt("SurveyDuration");
                                    final String table_name    = survey_listing.getString("SurveyTableName");

                                    // Get list of questions and their answers
                                    ParseQuery<ParseObject> query2 = new ParseQuery<ParseObject>(table_name);
                                    query2.findInBackground(new FindCallback<ParseObject>() {
                                        public void done(List<ParseObject> survey, ParseException e) {
                                            if (e == null) {
                                                int ques_ct = survey.size();
                                                List<Object> ques = new ArrayList<>();
                                                List<Object> ans = new ArrayList<>();
                                                List<Object> type = new ArrayList<>();
                                                List<Object> ansVals = new ArrayList<>();

                                                for (int j = 0; j < ques_ct; j++) {
                                                    ParseObject curr_ques = survey.get(j);
                                                    type.add(curr_ques.getString("QuestionType"));
                                                    ques.add(curr_ques.getString("Question"));
                                                    ans.add(j, Utilities.join(curr_ques.getList("AnswerArray"), "%%"));
                                                    ansVals.add(Utilities.join(curr_ques.getList("AnswerVals"), "%%"));
                                                }

                                                String ques_str = Utilities.join(ques, "%%");
                                                String type_str = Utilities.join(type, "%%");
                                                String ans_str = Utilities.join(ans, "%nxt%");
                                                String ansVal_str = Utilities.join(ansVals, "%nxt%");

                                                for(int j = 0; j < time.size(); j++) {
                                                    int hr  = Integer.parseInt(String.valueOf(time.get(j)).split(":")[0]);
                                                    int min = Integer.parseInt(String.valueOf(time.get(j)).split(":")[1]);

                                                    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                                                    Calendar cal = Calendar.getInstance();
                                                    cal.set(Calendar.HOUR_OF_DAY, hr);
                                                    cal.set(Calendar.MINUTE, min);
                                                    cal.set(Calendar.SECOND, 0);

                                                    Intent intent = new Intent(getApplicationContext(), PopupService.class);
                                                    intent.putExtra("ID", survey_id);
                                                    intent.putExtra("ITERATION", j);

                                                    PendingIntent pendingIntent = PendingIntent.getService(
                                                            getApplicationContext(),
                                                            Integer.parseInt(String.valueOf(survey_id) + String.valueOf(j)),
                                                            intent,
                                                            PendingIntent.FLAG_CANCEL_CURRENT);

                                                    for(int i = 0; i < 4; i++){
                                                        // Get alarm time
                                                        int curr_hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                                                        int curr_min = Calendar.getInstance().get(Calendar.MINUTE);

                                                        // If it's after the alarm time, schedule for next day
                                                        if ( curr_hr > hr || curr_hr == cal.get(Calendar.HOUR_OF_DAY)
                                                                && curr_min > cal.get(Calendar.MINUTE)) {
                                                            cal.add(Calendar.DAY_OF_YEAR, 1); // add, not set!
                                                        }

                                                        // Set alarm for survey pop-up to go off at default of 8:00 AM
                                                        alarmManager.setRepeating(
                                                                AlarmManager.RTC_WAKEUP,
                                                                cal.getTimeInMillis(),
                                                                alarmManager.INTERVAL_DAY,
                                                                pendingIntent);

                                                        cal.add(Calendar.MINUTE, duration/4);
                                                    }
                                                }


                                                dbHandler.createSurvey(
                                                        Utilities.join(time, ","),
                                                        duration,
                                                        name,
                                                        ques_str,
                                                        ans_str,
                                                        type_str,
                                                        ansVal_str
                                                );
                                            }
                                        }
                                    });

                                }
                                SharedPreferences.Editor edit = prefs.edit();
                                edit.putBoolean(getString(R.string.prev_started), Boolean.TRUE);
                                edit.commit();
                            }
                            else {
                                Log.i("Updates", "Parse initial survey request failed");
                            }
                        }
                    });
                }
            });
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

            TextView name = (TextView)arg1.findViewById(R.id.txtName);
            TextView time = (TextView)arg1.findViewById(R.id.txtTime);

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
                t_string = t_string + ", " + t0 + "-" + t1;

                // Check if survey is available
                Calendar curr_calendar = Calendar.getInstance();

                if ( curr_calendar.getTimeInMillis() > calendar0.getTimeInMillis() &&
                     curr_calendar.getTimeInMillis() < calendar1.getTimeInMillis()) {
                    accessible = true;
                }
            }

            // Set strings;
            time.setText(t_string);
            name.setText(dbHandler.getName(arg0 + 1));

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


