package com.example.natalie.android_wellbeing;

import android.app.ActionBar;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

/*
 * Created by Natalie on 2/16/2015.
 */

public class TEMP extends Activity {
    private TableLayout         surveyTable;
    private ArrayList<TableRow> ansRows   = new ArrayList<>();
    private ArrayList<CheckBox> ansChk    = new ArrayList<>();
    private ArrayList<SeekBar>  sliders   = new ArrayList<>();
    private ArrayList<TextView> sliderAns = new ArrayList<>();

    private int                 ID;
    private List<String>        ques_strs;
    private List<List<String>>  ans_str_lists;
    private List<List<Integer>> ans_val_lists;
    private List<List<String>>  ans_end_pts;
    private List<String>        ques_types;
    private List<Long>          tstamps;
    private List<Integer>       ansrs;
    private int                 ques_ct;
    private int []              ans_cts;
    private int                 ques_answered_ct  = 0;
    private int                 qNo = 0;
    private int                 vwNo = 0;
    private int []              q_in_view;
    private int                 maxVw;
    private long                lastTouch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*setContentView(R.layout.activity_survey_screen);

        lastTouch = Calendar.getInstance().getTimeInMillis();

        // Initialize Survey
        final SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());
        surveyTable = (TableLayout) findViewById(R.id.tableAns);
        final Button nextBttn    = (Button) findViewById(R.id.bttnNext);
        final Button prevBttn    = (Button) findViewById(R.id.bttnBack);

        // Retrieve survey's id
        Intent curr_intent = getIntent();
        ID = curr_intent.getIntExtra("ID", 1);

        // Initialize question variables
        ques_strs  = dbHandler.getQuesList(ID);
        ques_types = dbHandler.getQuesTypes(ID);
        ques_ct    = ques_strs.size();

        // Initialize answer variables
        ans_end_pts   = dbHandler.getEndPts(ID);
        ans_val_lists = dbHandler.getAnsVals(ID);
        ans_str_lists = dbHandler.getAnsLists(ID);
        ans_cts = new int[ques_ct];
        for (int i = 0; i < ques_ct; i++) {
            ans_cts[i] = ans_str_lists.get(i).size();
        }

        // Initialize answer and time-stamp output lists
        ansrs = dbHandler.getUserAns(ID);
        tstamps = dbHandler.getTStamps(ID);

        // Initialize survey view
        q_in_view = new int[ques_ct];
        int view = 0;
        for(int i = 0; i < ques_ct; i++){
            if(ques_types.get(i).equals("Button")){
                q_in_view[view] = 1;
                if((i+1) < ques_ct) {
                    view++;
                }
            }
            else if(ques_types.get(i).equals("Slider")){
                q_in_view[view]++;
                if((i+1) < ques_ct && (q_in_view[view] == 4 || ques_types.get(i+1).equals("Button"))){
                    view++;
                }
            }
        }
        maxVw = view;

        // On the event of the user clicking 'next'
        nextBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkSurveyActive()){
                    if(vwNo + 1 > maxVw)
                    {
                        Intent intent = new Intent(TEMP.this, FinishScreen.class);
                        updateAnsweredCount();

                        // Convert to object list in order to join by delimiter
                        List<Object> temp1 = new ArrayList<Object>();
                        List<Object> temp2 = new ArrayList<Object>();

                        temp1.addAll(ansrs);
                        temp2.addAll(tstamps);

                        // Store answers and timestamps
                        dbHandler.storeAnswers(Utilities.join(temp1, ","), ID);
                        dbHandler.storeTStamps(Utilities.join(temp2, ","), ID);

                        intent.putExtra("CT", ques_answered_ct);
                        intent.putExtra("ID", ID);

                        startActivityForResult(intent, 3);
                    }
                    else
                    {
                        vwNo++;
                        qNo++;
                        nextView();
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(), "Survey no longer active.", Toast.LENGTH_SHORT).show();
                    dbHandler.storeAnswers("empty", ID);
                    dbHandler.storeTStamps("empty", ID);

                    Intent intent = new Intent(TEMP.this, StartScreen.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }


            }
        });

        // On the event of the user clicking 'back'
        prevBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkSurveyActive()){
                    if(vwNo - 1 < 0) {
                        // Convert to object list in order to join by delimiter
                        List<Object> temp1 = new ArrayList<Object>();
                        List<Object> temp2 = new ArrayList<Object>();

                        temp1.addAll(ansrs);
                        temp2.addAll(tstamps);

                        // Store answers and timestamps
                        dbHandler.storeAnswers(Utilities.join(temp1, ","), ID);
                        dbHandler.storeTStamps(Utilities.join(temp2, ","), ID);

                        Intent intent = new Intent(TEMP.this, StartScreen.class);

                        startActivity(intent);
                    }
                    else {
                        int currVw_qCt = q_in_view[vwNo];
                        int prevVw_qCt = q_in_view[vwNo - 1];
                        qNo = qNo - currVw_qCt - prevVw_qCt + 1; // set qNo to first question of next view
                        vwNo--;
                        nextView();
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(), "Survey no longer active.", Toast.LENGTH_SHORT).show();
                    dbHandler.storeAnswers("empty", ID);
                    dbHandler.storeTStamps("empty", ID);

                    Intent intent = new Intent(TEMP.this, StartScreen.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
            }
        });

        // Go to first question
        nextView();
    }

    public void nextView() {

        // Clear TableLayout
        int num_rows = surveyTable.getChildCount();
        for(int i = 0; i < num_rows; i++) {
            surveyTable.removeAllViews();
        }

        // Clear Answer Row List and Slider Row List
        ansRows.clear();
        sliders.clear();
        sliderAns.clear();

        // Row Parameters
        TableLayout.LayoutParams rowParam = new TableLayout.LayoutParams();
        rowParam.height = TableRow.LayoutParams.MATCH_PARENT;
        rowParam.width = TableRow.LayoutParams.MATCH_PARENT;
        rowParam.bottomMargin = 10;

        // Centered Parameters
        TableRow.LayoutParams centerParam = new TableRow.LayoutParams();
        centerParam.height = TableRow.LayoutParams.MATCH_PARENT;
        centerParam.width = TableRow.LayoutParams.WRAP_CONTENT;
        centerParam.bottomMargin = 2;

        // Left-Align Parameters
        TableRow.LayoutParams leftParam = new TableRow.LayoutParams();
        leftParam.height = TableRow.LayoutParams.MATCH_PARENT;
        leftParam.width = TableRow.LayoutParams.WRAP_CONTENT;
        leftParam.bottomMargin = 2;

        // Button layout
        if(ques_types.get(qNo).equals("Button")) {
            // Create Question Row
            TableRow quesRow = new TableRow(this);
            quesRow.setLayoutParams(rowParam);

            TextView quesTxt = new TextView(this);
            quesTxt.setLayoutParams(leftParam);
            quesTxt.setTextSize(20);
            quesTxt.setTextColor(getResources().getColor(android.R.color.white));
            quesTxt.setText(ques_strs.get(qNo));

            quesRow.addView(quesTxt);
            surveyTable.addView(quesRow);

            // Create Answer Rows
            for(int i = 0; i < ans_cts[qNo]; i++) {
                final int curr_row = i;

                ansRows.add(new TableRow(this));
                ansRows.get(i).setLayoutParams(rowParam);
                ansRows.get(i).setBackgroundColor(getResources().getColor(R.color.survey_dark_grey));

                TextView ansTxt = new TextView(this);
                ansTxt.setLayoutParams(leftParam);
                ansTxt.setTextColor(getResources().getColor(android.R.color.white));
                ansTxt.setTextSize(20);
                ansTxt.setText(ans_str_lists.get(qNo).get(i));
                ansTxt.setPadding(10, 5, 5, 5);

                ansRows.get(i).addView(ansTxt);
                surveyTable.addView(ansRows.get(i));

                ansRows.get(i).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(checkSurveyActive()){
                            // Show answer selected
                            resetColors();
                            view.setBackgroundColor(getResources().getColor(android.R.color.black));

                            // If the answer changed, update time stamp
                            if (ansrs.get(qNo) != ans_val_lists.get(qNo).get(curr_row)) {
                                setTstamp(qNo);
                            }

                            // Record Answer
                            ansrs.set(qNo, ans_val_lists.get(qNo).get(curr_row));
                        }
                        else{
                            SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());
                            Toast.makeText(getApplicationContext(), "Survey no longer active.", Toast.LENGTH_SHORT).show();
                            dbHandler.storeAnswers("empty", ID);
                            dbHandler.storeTStamps("empty", ID);

                            Intent intent = new Intent(TEMP.this, StartScreen.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        }
                    }
                });
            }

            // Show answer selected previously;
            if(ansrs.get(qNo) != 0) {
                int ans = ansrs.get(qNo);
                ansRows.get(ans - 1).setBackgroundColor(getResources().getColor(android.R.color.black));
            }
        }
        /*
        // Checkbox layout
        if(ques_types.get(qNo).equals("Checkbox")) {
            // Create Question Row
            TableRow quesRow = new TableRow(this);
            quesRow.setLayoutParams(rowParam);

            TextView quesTxt = new TextView(this);
            quesTxt.setLayoutParams(leftParam);
            quesTxt.setTextSize(20);
            quesTxt.setTextColor(getResources().getColor(android.R.color.white));
            quesTxt.setText(ques_strs.get(qNo));

            quesRow.addView(quesTxt);
            surveyTable.addView(quesRow);

            // Create Answer Rows
            for(int i = 0; i < ans_cts[qNo]; i++) {
                final int curr_row = i;

                ansRows.add(new TableRow(this));
                ansRows.get(i).setLayoutParams(rowParam);
                ansRows.get(i).setBackgroundColor(getResources().getColor(R.color.survey_dark_grey));

                ansChk.add(new CheckBox(this));
                ansChk.get(i).setText(ans_str_lists.get(qNo).get(i));
                ansChk.get(i).setLayoutParams(leftParam);
                ansChk.get(i).setTextColor(getResources().getColor(android.R.color.white));
                ansChk.get(i).setTextSize(20);
                ansChk.get(i).setPadding(10, 5, 5, 5);

                ansRows.get(i).addView(ansChk.get(i));
                surveyTable.addView(ansRows.get(i));

                ansChk.get(i).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(checkSurveyActive()){
                            // Show answer selected
                            // If the answer changed, update time stamp
                            if (ansrs.get(qNo) != ans_val_lists.get(qNo).get(curr_row)) {
                                setTstamp(qNo);
                            }

                            // Record Answer
                            ansrs.set(qNo, ans_val_lists.get(qNo).get(curr_row));
                        }
                        else{
                            SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());
                            Toast.makeText(getApplicationContext(), "Survey no longer active.", Toast.LENGTH_SHORT).show();
                            dbHandler.storeAnswers("empty", ID);
                            dbHandler.storeTStamps("empty", ID);

                            Intent intent = new Intent(SurveyScreen.this, StartScreen.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        }
                    }
                });
            }

            // Show answer selected previously;
            if(ansrs.get(qNo) != 0) {
                int ans = ansrs.get(qNo);
                ansRows.get(ans - 1).setBackgroundColor(getResources().getColor(android.R.color.black));
            }
        }
        // Slider layout
        else if (ques_types.get(qNo).equals("Slider")) {
            int sliderCt = 0;
            final int topQues = qNo;
            String endpt1 = ans_end_pts.get(qNo).get(0);
            String endpt2 = ans_end_pts.get(qNo).get(1);

            // Create Scale Row
            if(!endpt1.equals("-")) {
                TableRow endPtsRow = new TableRow(this);
                endPtsRow.setLayoutParams(rowParam);

                TextView endPtsTxt = new TextView(this);
                endPtsTxt.setLayoutParams(centerParam);
                endPtsTxt.setTextColor(getResources().getColor(android.R.color.white));
                endPtsTxt.setTextSize(20);
                endPtsTxt.setText("SCALE:\n\"" + endpt1 + "\" to \"" + endpt2 + "\"");
                endPtsTxt.setPadding(0, 0, 0, 10);

                endPtsRow.addView(endPtsTxt);
                surveyTable.addView(endPtsRow);
            }


            for(int i = qNo; i < ques_ct && sliderCt < 4; i++) {
                if(ques_types.get(i).equals("Slider")) {
                    // Create Question Row
                    TableRow quesRow = new TableRow(this);
                    quesRow.setLayoutParams(rowParam);

                    TextView quesTxt = new TextView(this);
                    quesTxt.setLayoutParams(leftParam);
                    quesTxt.setTextColor(getResources().getColor(android.R.color.white));
                    quesTxt.setTextSize(20);
                    quesTxt.setText(ques_strs.get(i));

                    quesRow.addView(quesTxt);
                    surveyTable.addView(quesRow);

                    // Create Slider Row
                    TableRow sliderRow = new TableRow(this);
                    sliderRow.setLayoutParams(rowParam);

                    TableRow ansRow = new TableRow(this);
                    ansRow.setLayoutParams(rowParam);

                    sliders.add(new SeekBar(this));
                    TableRow.LayoutParams sliderParam = new TableRow.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ActionBar.LayoutParams.WRAP_CONTENT,
                            1);

                    sliderRow.addView(sliders.get(sliderCt), sliderParam);

                    // Create TextView Row
                    sliderAns.add(new TextView(this));
                    sliderAns.get(sliderCt).setLayoutParams(centerParam);
                    ansRow.addView(sliderAns.get(sliderCt));

                    surveyTable.addView(ansRow);
                    surveyTable.addView(sliderRow);


                    // Initialize Slider
                    sliders.get(sliderCt).setProgress(0);
                    sliders.get(sliderCt).setMax(ans_cts[i]);

                    // Initialize TextView
                    int index = ans_val_lists.get(qNo + sliderCt).indexOf(ansrs.get(i)); // get index of user's answer in answer list

                    if(index != -1) {
                        sliders.get(sliderCt).incrementProgressBy(index + 1); // set progress of slider to user's answer
                        sliderAns.get(sliderCt).setText(ans_str_lists.get(topQues + sliderCt).get(index)); // set answer text
                        sliderAns.get(sliderCt).setTextColor(getResources().getColor(android.R.color.white));
                        sliderAns.get(sliderCt).setGravity(Gravity.CENTER_HORIZONTAL);
                    }
                    else {
                        sliders.get(sliderCt).incrementProgressBy(0); // set progress of slider to user's answer
                        sliderAns.get(sliderCt).setText("");          // set answer text
                        sliderAns.get(sliderCt).setTextColor(getResources().getColor(android.R.color.white));
                        sliderAns.get(sliderCt).setGravity(Gravity.CENTER_HORIZONTAL);
                    }


                    // Create Slider Listener
                    final int sliderOffset = sliderCt;
                    sliders.get(sliderCt).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int j, boolean b) {
                            if(checkSurveyActive()){
                                int curr_ans = ansrs.get(topQues + sliderOffset);
                                int new_ans;
                                if(j != 0) {
                                    new_ans = ans_val_lists.get(topQues + sliderOffset).get(j - 1);
                                }
                                else {
                                    sliders.get(sliderOffset).setProgress(1);
                                    new_ans = ans_val_lists.get(topQues + sliderOffset).get(0);
                                }

                                if(curr_ans != new_ans) {
                                    setTstamp(topQues + sliderOffset);
                                }

                                if(j != 0 ) {
                                    sliderAns.get(sliderOffset).setText(ans_str_lists.get(topQues + sliderOffset).get(j - 1));
                                    ansrs.set(topQues + sliderOffset, new_ans);
                                }
                                else {
                                    sliderAns.get(sliderOffset).setText(ans_str_lists.get(topQues + sliderOffset).get(0));
                                    ansrs.set(topQues + sliderOffset, new_ans);
                                }
                            }
                            else{
                                SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());
                                Toast.makeText(getApplicationContext(), "Survey no longer active.", Toast.LENGTH_SHORT).show();
                                dbHandler.storeAnswers("empty", ID);
                                dbHandler.storeTStamps("empty", ID);

                                Intent intent = new Intent(TEMP.this, StartScreen.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });

                    sliderCt++; // increment count of sliders on view
                }
                else break;
            }

            qNo = qNo + sliderCt - 1;      // increment current question number
        }*/
    }

    public boolean checkSurveyActive(){
        long curr_time = Calendar.getInstance().getTimeInMillis();
        long t_diff = curr_time - lastTouch;

        if(Utilities.validTime(getApplicationContext(), ID)){
            Log.i("TIME>>>", "valid time");
            lastTouch = curr_time;
            return true;
        }
        else{
            if((t_diff/1000)/60 <= 5){
                Log.i("TIME>>>", "less than 5 minutes inactive");
                lastTouch = curr_time;
                return true;
            }
            else{
                Log.i("TIME>>>", "inactive");
                return false;
            }
        }
    }

    public void resetColors() {
        int curr_ans_ct = ans_cts[qNo];
        for(int i = 0; i < curr_ans_ct; i++){
            ansRows.get(i).setBackgroundColor(getResources().getColor(R.color.survey_dark_grey));
        }
    }

    public void updateAnsweredCount() {
        int cnt = 0;
        for(int i = 0; i < ques_ct; i++) {
            if(ansrs.get(i) != 0) {
                cnt++;
            }
        }
        ques_answered_ct = cnt;
    }

    public void setTstamp(int num) {
        tstamps.set(num, System.currentTimeMillis() / 1000L);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /*if (requestCode == 3 && data != null) {
            // Get answer and timestamp array previously passed to Finish activity
            SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());

            ID = data.getIntExtra("ID", 1);
            ansrs = dbHandler.getUserAns(ID);
            tstamps = dbHandler.getTStamps(ID);

            qNo = ques_ct - q_in_view[maxVw];
            vwNo = maxVw;

            nextView();
        }*/
    }

    @Override
    public void onBackPressed() {
        /*SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());
        if(checkSurveyActive()){
            if(vwNo - 1 < 0) {
                // Convert to object list in order to join by delimiter
                List<Object> temp1 = new ArrayList<Object>();
                List<Object> temp2 = new ArrayList<Object>();

                temp1.addAll(ansrs);
                temp2.addAll(tstamps);

                // Store answers and timestamps
                dbHandler.storeAnswers(Utilities.join(temp1, ","), ID);
                dbHandler.storeTStamps(Utilities.join(temp2, ","), ID);

                Intent intent = new Intent(TEMP.this, StartScreen.class);

                startActivity(intent);
            }
            else {
                int currVw_qCt = q_in_view[vwNo];
                int prevVw_qCt = q_in_view[vwNo - 1];
                qNo = qNo - currVw_qCt - prevVw_qCt + 1; // set qNo to first question of next view
                vwNo--;
                nextView();
            }
        }else{
            Toast.makeText(getApplicationContext(), "Survey no longer active.", Toast.LENGTH_SHORT).show();
            dbHandler.storeAnswers("empty", ID);
            dbHandler.storeTStamps("empty", ID);

            Intent intent = new Intent(TEMP.this, StartScreen.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }*/



    }
}

