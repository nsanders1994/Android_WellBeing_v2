package com.example.natalie.android_wellbeing;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/*
 * Created by Natalie on 2/16/2015.
 */

public class SurveyScreen extends Activity {
    private TableLayout         surveyTable;
    private ArrayList<TableRow> ansRows   = new ArrayList<>();
    private ArrayList<SeekBar>  sliders   = new ArrayList<>();
    private ArrayList<TextView> sliderAns = new ArrayList<>();

    private int                 ID;
    private List<String>        ques_strs;
    private List<List<String>>  ans_str_lists;
    private List<List<Integer>> ans_val_lists;
    private List<String>        ques_types;
    private List<Long>          tstamps;
    private List<Integer>       ansrs;
    private int                 ques_ct;
    private int  []             ans_cts;
    private int                 ques_answered_ct  = 0;
    private int                 qNo = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_screen);

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
        ans_val_lists = dbHandler.getAnsVals(ID);
        ans_str_lists = dbHandler.getAnsLists(ID);
        ans_cts = new int[ques_ct];
        for (int i = 0; i < ques_ct; i++) {
            ans_cts[i] = ans_str_lists.get(i).size();
        }

        // Initialize answer and time-stamp output lists
        ansrs = dbHandler.getUserAns(ID);
        /*ansrs = new ArrayList<Integer>(ques_ct); //= new int[ques_ct];

        List<Integer> stored_ansrs = dbHandler.getUserAns(ID);
        int ct = stored_ansrs.size();
        for(int j = 0; j < ct; j++){
            ansrs[j] = stored_ansrs.get(j);
        }*/

        //tstamps = new long[ques_ct];
        tstamps = dbHandler.getTStamps(ID);


        // On the event of the user clicking 'next'
        nextBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(qNo + 1 == ques_ct)
                {
                    Intent intent = new Intent(SurveyScreen.this, FinishScreen.class);
                    updateAnsweredCount();

                    // Convert to object list in order to join by delimiter
                    List<Object> temp1 = new ArrayList<Object>();
                    List<Object> temp2 = new ArrayList<Object>();

                    temp1.addAll(ansrs);
                    temp2.addAll(tstamps);

                    // Store answers and timestamps
                    dbHandler.storeAnswers(Utilities.join(temp1, ","), ID);
                    dbHandler.storeTStamps(Utilities.join(temp2, ","), ID);

                    /*
                    Integer[] temp = new Integer[ques_ct];
                    ansrs.toArray(temp);

                    intent.putExtra("ANS", ansrs.toArray());
                    Log.i("DEBUG>>>>>", "In Survey, ans = " + temp);
                    intent.putExtra("TSTAMP", temp);*/
                    intent.putExtra("CT", ques_answered_ct);
                    intent.putExtra("ID", ID);

                    startActivityForResult(intent, 3);
                }
                else
                {
                    Log.i("DEBUG>>>> ", "On next: before inc = " + String.valueOf(qNo));
                    qNo++;
                    Log.i("DEBUG>>>> ", "On next: after inc = " + String.valueOf(qNo));

                    nextView();
                }
            }
        });

        // On the event of the user clicking 'back'
        prevBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(qNo - 1 == -1) {
                    // Convert to object list in order to join by delimiter
                    List<Object> temp1 = new ArrayList<Object>();
                    List<Object> temp2 = new ArrayList<Object>();

                    temp1.addAll(ansrs);
                    temp2.addAll(tstamps);

                    // Store answers and timestamps
                    dbHandler.storeAnswers(Utilities.join(temp1, ","), ID);
                    dbHandler.storeTStamps(Utilities.join(temp2, ","), ID);

                    Intent intent = new Intent(SurveyScreen.this, StartScreen.class);
                    startActivityForResult(intent, 2);
                }
                else {
                    /*
                    if(ques_types.get(qNo--) == "slider") {
                        int num_rows = surveyTable.getChildCount();
                        int inview_ques_ct = num_rows/3;
                        for(int i = 0; i < inview_ques_ct; i++) {
                            qNo--;
                        }
                    }*/
                    //else
                    qNo--;
                    nextView();
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
        rowParam.bottomMargin = 2;

        // Centered Parameters
        TableRow.LayoutParams centerParam = new TableRow.LayoutParams();
        centerParam.height = TableRow.LayoutParams.MATCH_PARENT;
        centerParam.width = TableRow.LayoutParams.MATCH_PARENT;
        centerParam.gravity = Gravity.CENTER_HORIZONTAL;
        centerParam.bottomMargin = 2;

        // Left-Align Parameters
        TableRow.LayoutParams leftParam = new TableRow.LayoutParams();
        leftParam.height = TableRow.LayoutParams.MATCH_PARENT;
        leftParam.width = TableRow.LayoutParams.WRAP_CONTENT;
        leftParam.bottomMargin = 2;

        Log.i("DEBUG>>>>", String.valueOf(qNo));
        Log.i("DEBUG>>>>", ques_types.get(qNo));
        // Button layout
        if(ques_types.get(qNo).equals("Button")) {
            Log.i("DEBUG>>>>", "creating a button survey");
            // Create Question Row
            TableRow quesRow = new TableRow(this);
            quesRow.setLayoutParams(rowParam);

            TextView quesTxt = new TextView(this);
            quesTxt.setLayoutParams(leftParam);
            quesTxt.setText(ques_strs.get(qNo));

            quesRow.addView(quesTxt);
            surveyTable.addView(quesRow);

            // Create Answer Rows
            for(int i = 0; i < ans_cts[qNo]; i++) {
                final int curr_row = i;

                ansRows.add(new TableRow(this));
                ansRows.get(i).setLayoutParams(leftParam);
                ansRows.get(i).setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));

                TextView ansTxt = new TextView(this);
                ansTxt.setLayoutParams(leftParam);
                ansTxt.setText(ans_str_lists.get(qNo).get(i));

                ansRows.get(i).addView(ansTxt);
                surveyTable.addView(ansRows.get(i));

                ansRows.get(i).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Show answer selected
                        resetColors();
                        view.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));

                        // If the answer changed, update time stamp
                        if (ansrs.get(qNo) != curr_row) {
                            setTstamp(qNo);
                        };

                        // Record Answer
                        ansrs.set(qNo, ans_val_lists.get(qNo).get(curr_row));
                    }
                });
            }

            // Show answer selected previously;
            if(ansrs.get(qNo) != 0) {
                int ans = ansrs.get(qNo);
                ansRows.get(ans - 1).setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        }
        // Slider layout
        else if (ques_types.get(qNo).equals("Slider")) {
            Log.i("DEBUG>>>>>>", "Creating slider survey");
            int sliderCt = 0;
            final int topQues = qNo;
            for(int i = qNo; i < ques_ct && sliderCt < 4; i++) {
                if(ques_types.get(i).equals("Slider")) {
                    // Create Question Row
                    TableRow quesRow = new TableRow(this);
                    quesRow.setLayoutParams(rowParam);

                    TextView quesTxt = new TextView(this);
                    quesTxt.setLayoutParams(leftParam);
                    quesTxt.setText(ques_strs.get(i));

                    quesRow.addView(quesTxt);
                    surveyTable.addView(quesRow);

                    // Create Slider Row
                    TableRow sliderRow = new TableRow(this);
                    sliderRow.setLayoutParams(rowParam);

                    TableRow ansRow = new TableRow(this);
                    ansRow.setLayoutParams(rowParam);

                    sliders.add(new SeekBar(this));
                    sliders.get(sliderCt).setLayoutParams(centerParam);
                    sliderRow.addView(sliders.get(sliderCt));

                    // Create TextView Row
                    sliderAns.add(new TextView(this));
                    sliderAns.get(sliderCt).setLayoutParams(centerParam);
                    ansRow.addView(sliderAns.get(sliderCt));

                    surveyTable.addView(sliderRow);
                    surveyTable.addView(ansRow);

                    // Initialize Slider
                    sliders.get(sliderCt).setProgress(0);
                    sliders.get(sliderCt).setMax(ans_cts[i]);
                    Log.i("DEBUG>>>>>>", "ans_cts[" + String.valueOf(i) + "] = " + String.valueOf(ans_cts[i]));

                    // Initialize TextView
                    int index = ans_val_lists.get(qNo).indexOf(ansrs.get(i)); // get index of user's answer in answer list

                    if(index != -1) {
                        sliders.get(sliderCt).incrementProgressBy(index + 1); // set progress of slider to user's answer
                        sliderAns.get(sliderCt).setText(ans_str_lists.get(topQues + sliderCt).get(index)); // set answer text
                    }
                    else {
                        sliders.get(sliderCt).incrementProgressBy(0); // set progress of slider to user's answer
                        sliderAns.get(sliderCt).setText("");          // set answer text
                    }


                    // Create Slider Listener
                    final int sliderOffset = sliderCt;
                    sliders.get(sliderCt).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int j, boolean b) {
                            Log.i("DEBUG>>>>>", "In slider listener: sliderOffset = " + String.valueOf(sliderOffset) + " topQues = " + String.valueOf(topQues) + " j = " + String.valueOf(j) );

                            setTstamp(topQues + sliderOffset);

                            if(j != 0 ) {
                                sliderAns.get(sliderOffset).setText(ans_str_lists.get(topQues + sliderOffset).get(j - 1));
                                ansrs.set(topQues + sliderOffset, ans_val_lists.get(topQues + sliderOffset).get(j - 1));
                            }
                            else {
                                sliderAns.get(sliderOffset).setText("");
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
        }
    }

    public void resetColors() {
        int curr_ans_ct = ans_cts[qNo];
        for(int i = 0; i < curr_ans_ct; i++){
            ansRows.get(i).setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
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
        Log.i("DEBUG>>>>>>>>>>>", "qNo is " + String.valueOf(qNo));

        if(ansrs.get(num) != 0) {
            tstamps.set(num, System.currentTimeMillis() / 1000L);
        }
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 3 && data != null) {

            // Get answer and timestamp array previously passed to Finish activity
            SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());

            ID = data.getIntExtra("ID", 1);
            ansrs = dbHandler.getUserAns(ID);
            tstamps = dbHandler.getTStamps(ID);

            qNo = ques_ct - 1;

            // Reset selection of table view
            nextView();
        }
    }
}

