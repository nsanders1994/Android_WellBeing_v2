package com.example.natalie.android_wellbeing;

import android.app.ActionBar;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Natalie on 2/16/2015.
**/

public class SurveyScreen extends Activity {
    /**
     * This activity displays the each question in the survey. To move between questions the user uses
     * the 'back' and 'next buttons. When a new question needs to be displayed, the activity clears the
     * TableView and programmatically builds the view for the new question. One Checkbox, Textbox,
     * or Button question may be displayed per page while a maximum of three Slider questions can be
     * displayed in one view.
     **/

     private TableLayout         surveyTable;                   // TableView within the layout that holds all question info
     private ArrayList<TableRow> ansRows   = new ArrayList<>(); // TableRows used for answer info
     private ArrayList<CheckBox> ansChk    = new ArrayList<>(); // CheckBoxes used for CheckBox question
     private ArrayList<SeekBar>  sliders   = new ArrayList<>(); // SeekBars (sliders) used for Slider questions
     private ArrayList<TextView> sliderAns = new ArrayList<>(); // TextViews for the slider options
     private EditText            textbox;                       // EditText used for Textbox

     private TableLayout.LayoutParams rowParam;                 // Layout parameters for table rows
     private TableRow.LayoutParams    centerParam;              // Centered parameters for data in table row
     private TableRow.LayoutParams    leftParam;

     private int                 ID;
     private List<String>        ques_strs;
     private List<List<String>>  ans_str_lists;
     private List<List<Integer>> ans_val_lists;
     private List<List<String>>  ans_end_pts;
     private List<String>        ques_types;
     private List<Long>          tstamps;
     private List<String>        ansrs;
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
        setContentView(R.layout.activity_survey_screen);

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (!mWifi.isConnected()) {
            Toast.makeText(getApplicationContext(),
                    "You have no WiFi! You will not be able to submit this survey",
                    Toast.LENGTH_SHORT).show();
        }

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

            if(ques_types.get(i).equals("Slider")){
                q_in_view[view]++;
                if((i+1) < ques_ct && (q_in_view[view] == 3 || ques_types.get(i+1).equals("Button"))){
                    view++;
                }
            }
            else{
                q_in_view[view] = 1;
                if((i+1) < ques_ct) {
                    view++;
                }
            }
        }
        maxVw = view;

        // Initialize parameters
        // Row Parameters
        rowParam = new TableLayout.LayoutParams();
        rowParam.height = TableRow.LayoutParams.MATCH_PARENT;
        rowParam.width = TableRow.LayoutParams.MATCH_PARENT;
        rowParam.bottomMargin = 10;

        // Centered Parameters
        centerParam = new TableRow.LayoutParams();
        centerParam.height = TableRow.LayoutParams.MATCH_PARENT;
        centerParam.width = TableRow.LayoutParams.WRAP_CONTENT;
        centerParam.bottomMargin = 2;

        // Left-Align Parameters
        leftParam = new TableRow.LayoutParams();
        leftParam.height = TableRow.LayoutParams.MATCH_PARENT;
        leftParam.width = TableRow.LayoutParams.WRAP_CONTENT;
        leftParam.bottomMargin = 2;

        // On the event of the user clicking 'next'
        nextBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkSurveyActive()){
                    Log.i("DEBUG>>", "maxVw = " + String.valueOf(maxVw));
                    Log.i("DEBUG>>", "vwN0 + 1 = " + String.valueOf(vwNo + 1));

                    if(vwNo + 1 > maxVw)
                    {
                        Intent intent = new Intent(SurveyScreen.this, FinishScreen.class);
                        updateAnsweredCount();

                        // Convert to object list in order to join by delimiter
                        List<Object> temp1 = new ArrayList<Object>();
                        List<Object> temp2 = new ArrayList<Object>();

                        temp1.addAll(ansrs);
                        temp2.addAll(tstamps);

                        // Store answers and timestamps
                        dbHandler.storeAnswers(Utilities.join(temp1, "`nxt`"), ID);
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

                    Intent intent = new Intent(SurveyScreen.this, StartScreen.class);
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
                        dbHandler.storeAnswers(Utilities.join(temp1, "`nxt`"), ID);
                        dbHandler.storeTStamps(Utilities.join(temp2, ","), ID);

                        Intent intent = new Intent(SurveyScreen.this, StartScreen.class);

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

                    Intent intent = new Intent(SurveyScreen.this, StartScreen.class);
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
        ansChk.clear();



        // Button layout
        if(ques_types.get(qNo).equals("Button")) {
            createButtonVw();
        }

        // Checkbox layout
        if(ques_types.get(qNo).equals("Checkbox")) {
            createCheckboxVw();
        }

        // Slider layout
        else if (ques_types.get(qNo).equals("Slider")) {
            createSliderVw();
        }

        // Textbox layout
        else if (ques_types.get(qNo).equals("Textbox")) {
            createTextboxVw();
        }
    }

    public void createButtonVw(){
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
                        String ans_selected = String.valueOf(ans_val_lists.get(qNo).get(curr_row));
                        if (!ansrs.get(qNo).equals(ans_selected)) {
                            setTstamp(qNo);
                        }

                        // Record Answer
                        ansrs.set(qNo, ans_selected);
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
        if(!ansrs.get(qNo).equals("0")) {
            int selected_row = Integer.parseInt(ansrs.get(qNo)) - 1;
            ansRows.get(selected_row).setBackgroundColor(getResources().getColor(android.R.color.black));
        }
    }

    public void createCheckboxVw(){
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

            ansChk.add(new CheckBox(this));
            ansChk.get(i).setText("      " + ans_str_lists.get(qNo).get(i));
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
                        // Update time stamp and answer string
                        setTstamp(qNo);

                        String new_ans = "0";
                        for(int k = 0; k < ans_cts[qNo]; k++){
                            if(ansChk.get(k).isChecked()){
                                if(new_ans.equals("0")){
                                    new_ans = String.valueOf(ans_val_lists.get(qNo).get(k));
                                }
                                else{
                                    new_ans = new_ans + "," + String.valueOf(ans_val_lists.get(qNo).get(k));
                                }
                            }
                        }

                        // Record answer
                        ansrs.set(qNo, new_ans);
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
        String checked_ans [] = ansrs.get(qNo).split(",");
        for(int k = 0; k < checked_ans.length; k++){
            int boxNum = Integer.parseInt(checked_ans[k]) - 1;
            if(!checked_ans[k].equals("0")) {
                ansChk.get(boxNum).setChecked(true);
            }
        }
    }

    public void createSliderVw(){
        int sliderCt = 0;
        final int topQues = qNo;
        String endpt1 = ans_end_pts.get(qNo).get(0);
        String endpt2 = ans_end_pts.get(qNo).get(1);

        // Create Row Specifying Slider Scale
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

        // Create up to 3 Sliders in Current View
        for(int i = qNo; i < ques_ct && sliderCt < 3; i++) {
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

                // Create TextView Row for Answer
                sliderAns.add(new TextView(this));
                sliderAns.get(sliderCt).setLayoutParams(centerParam);
                ansRow.addView(sliderAns.get(sliderCt));

                surveyTable.addView(ansRow);
                surveyTable.addView(sliderRow);

                // Initialize Slider Progress and Max
                sliders.get(sliderCt).setProgress(0);
                sliders.get(sliderCt).setMax(ans_cts[i]);

                // Initialize Answer
                int index = ans_val_lists.get(qNo + sliderCt).indexOf(Integer.parseInt(ansrs.get(i))); // get index of user's answer in answer list

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
                            String curr_ans = ansrs.get(topQues + sliderOffset);
                            int new_ans;
                            if(j != 0) {
                                new_ans = ans_val_lists.get(topQues + sliderOffset).get(j - 1);
                            }
                            else {
                                sliders.get(sliderOffset).setProgress(1);
                                new_ans = ans_val_lists.get(topQues + sliderOffset).get(0);
                            }

                            if(!curr_ans.equals(String.valueOf(new_ans))) {
                                setTstamp(topQues + sliderOffset);
                            }

                            if(j != 0 ) {
                                sliderAns.get(sliderOffset).setText(ans_str_lists.get(topQues + sliderOffset).get(j - 1));
                                ansrs.set(topQues + sliderOffset, String.valueOf(new_ans));
                            }
                            else {
                                sliderAns.get(sliderOffset).setText(ans_str_lists.get(topQues + sliderOffset).get(0));
                                ansrs.set(topQues + sliderOffset, String.valueOf(new_ans));
                            }
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

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                sliderCt++; // increment count of sliders on view
            }
            else break;
        }

        qNo = qNo + sliderCt - 1; // increment current question number
    }

    public void createTextboxVw(){
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

        //
        TableRow.LayoutParams textboxParam = new TableRow.LayoutParams();
        textboxParam.height = 500;
        textboxParam.width = TableRow.LayoutParams.WRAP_CONTENT;
        textboxParam.bottomMargin = 2;

        // Create Textbox Row
        ansRows.add(new TableRow(this));
        ansRows.get(0).setLayoutParams(rowParam);

        textbox = new EditText(this);
        textbox.setLayoutParams(textboxParam);
        textbox.setPadding(10, 5, 5, 5);
        textbox.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        textbox.setBackgroundColor(getResources().getColor(R.color.survey_dark_grey));
        textbox.setTextColor(getResources().getColor(android.R.color.white));
        textbox.setGravity(Gravity.TOP);

        ansRows.get(0).addView(textbox);
        surveyTable.addView(ansRows.get(0));

        textbox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if(checkSurveyActive()){
                    // Update time stamp
                    setTstamp(qNo);

                    // Record Answer
                    String ansTxt = textbox.getText().toString();
                    ansrs.set(qNo, ansTxt);
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

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // Show answer given previously;
        if(!ansrs.get(qNo).equals("0")) {
            String ans = ansrs.get(qNo);
            textbox.setText(ans);
            textbox.setSelection(ans.length());

        }
    }

    public boolean checkSurveyActive(){
        long curr_time = Calendar.getInstance().getTimeInMillis();
        long t_diff = curr_time - lastTouch;

        if(Utilities.surveyOpen(getApplicationContext(), ID)){
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
            if(!ansrs.get(i).equals("0")) {
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

        if (requestCode == 3 && data != null) {
            // Get answer and timestamp array previously passed to Finish activity
            SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());

            ID = data.getIntExtra("ID", 1);
            ansrs = dbHandler.getUserAns(ID);
            tstamps = dbHandler.getTStamps(ID);

            qNo = ques_ct - q_in_view[maxVw];
            Log.i("DEBUG>>", "On return maxVw = " + String.valueOf(maxVw));
            vwNo = maxVw;

            nextView();
        }
    }

    @Override
    public void onBackPressed() {
        SurveyDatabaseHandler dbHandler = new SurveyDatabaseHandler(getApplicationContext());
        if(checkSurveyActive()){
            if(vwNo - 1 < 0) {
                // Convert to object list in order to join by delimiter
                List<Object> temp1 = new ArrayList<Object>();
                List<Object> temp2 = new ArrayList<Object>();

                temp1.addAll(ansrs);
                temp2.addAll(tstamps);

                // Store answers and timestamps
                dbHandler.storeAnswers(Utilities.join(temp1, "`nxt`"), ID);
                dbHandler.storeTStamps(Utilities.join(temp2, ","), ID);

                Intent intent = new Intent(SurveyScreen.this, StartScreen.class);

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

            Intent intent = new Intent(SurveyScreen.this, StartScreen.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }
}

