package com.example.natalie.android_wellbeing;

/**
 * Created by Natalie on 2/3/2015.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SurveyDatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION   = 4;
    private static final String DATABASE_NAME   = "WellbeingManager";
    private static final String TABLE_WELLBEING = "Wellbeing";
    private static final String KEY_POPUP_TIME  = "PopupHr";
    private static final String KEY_DURATION    = "Duration";
    private static final String KEY_NAME        = "Name";
    private static final String KEY_QUES        = "Questions";
    private static final String KEY_ANSRS       = "Answers";
    private static final String KEY_TYPES       = "Types";
    private static final String KEY_COMPLETE    = "Complete";
    private static final String KEY_USER_ANS    = "UserAns";
    private static final String KEY_ANS_VALS    = "AnsVals";
    private static final String KEY_TSTAMPS     = "TStamps";
    private static final String KEY_VERSION     = "Version";

    public SurveyDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_WELLBEING + "("
                + KEY_POPUP_TIME  + " STRING,"
                + KEY_DURATION    + " INTEGER,"
                + KEY_NAME        + " STRING,"
                + KEY_QUES        + " STRING,"
                + KEY_ANSRS       + " STRING,"
                + KEY_TYPES       + " STRING,"
                + KEY_COMPLETE    + " INTEGER,"
                + KEY_USER_ANS    + " STRING,"
                + KEY_ANS_VALS    + " STRING,"
                + KEY_TSTAMPS     + " STRING,"
                + KEY_VERSION     + " INTEGER)");
    }

    public void createSurvey(String times, int dur, String name, String ques, String ans, String types, String ansVals, int version) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_POPUP_TIME, times);
        values.put(KEY_DURATION,   dur);
        values.put(KEY_NAME,       name);
        values.put(KEY_QUES,       ques);
        values.put(KEY_ANSRS,      ans);
        values.put(KEY_TYPES,      types);
        values.put(KEY_COMPLETE,   0);
        values.put(KEY_USER_ANS,   "empty");
        values.put(KEY_ANS_VALS,   ansVals);
        values.put(KEY_TSTAMPS,    "empty");
        values.put(KEY_VERSION,    version);

        db.insert(TABLE_WELLBEING,null,values);
        db.close();
    }

    public void deleteAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete * from "+ TABLE_WELLBEING);
    }

    public int getSurveyCount(){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_WELLBEING, null);

        int count = cursor.getCount();
        cursor.close();
        db.close();

        return count;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WELLBEING);

        onCreate(db);
    }

    public List<Integer> getSurveyIDs() {
        List<Integer> ids = new ArrayList<Integer>();

        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT rowid FROM " + TABLE_WELLBEING, null);

        if (cursor.moveToFirst()) {
            do {
                ids.add(cursor.getInt(0));
            }while(cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return ids;
    }

    public List<String> getTimes(int id){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + KEY_POPUP_TIME +
                        " FROM " + TABLE_WELLBEING +
                        " WHERE rowid = " + id,
                null
        );

        cursor.moveToFirst();
        String time_str = cursor.getString(0); //Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_POPUP_HR)));
        List<String> time_list = Arrays.asList(time_str.split(","));

        cursor.close();
        db.close();

        return time_list;
    }


    public int getDuration(int id){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + KEY_DURATION +
                        " FROM " + TABLE_WELLBEING +
                        " WHERE rowid = " + id,
                null
        );

        cursor.moveToFirst();
        int dur = cursor.getInt(0); //Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_POPUP_MIN)));

        cursor.close();
        db.close();

        return dur;
    }

    public String getName(int id){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + KEY_NAME +
                        " FROM " + TABLE_WELLBEING +
                        " WHERE rowid = " + id,
                null
        );

        cursor.moveToFirst();
        String name = cursor.getString(0);

        cursor.close();
        db.close();

        return name;
    }

    public List <String> getQuesList(int id){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + KEY_QUES +
                        " FROM " + TABLE_WELLBEING +
                        " WHERE rowid = " + id,
                null
        );

        cursor.moveToFirst();
        String ques_str = cursor.getString(0); //cursor.getString(cursor.getColumnIndex(KEY_BASE_QUES));
        List<String> ques_list = Arrays.asList(ques_str.split("%%"));

        cursor.close();
        db.close();

        return ques_list;
    }

    public List< List<String> > getAnsLists(int id){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + KEY_ANSRS +
                        " FROM " + TABLE_WELLBEING +
                        " WHERE rowid = " + id,
                null
        );

        cursor.moveToFirst();
        String ans_str = cursor.getString(0);
        String [] ans_array = ans_str.split("%nxt%");

        int ques_ct = ans_array.length;
        List< List<String> > ans_list = new ArrayList<List<String>>();
        for(int i = 0; i < ques_ct; i++) {
            ans_list.add(i, Arrays.asList(ans_array[i].split("%%")));
        }

        cursor.close();
        db.close();

        return ans_list;
    }

    public List<String> getQuesTypes(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + KEY_TYPES +
                        " FROM " + TABLE_WELLBEING +
                        " WHERE rowid = " + id,
                null
        );

        cursor.moveToFirst();
        String type_str = cursor.getString(0);
        List <String> type_list = Arrays.asList(type_str.split("%%"));

        cursor.close();
        db.close();

        return type_list;
    }

    public boolean isCompleted(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + KEY_COMPLETE +
                        " FROM " + TABLE_WELLBEING +
                        " WHERE rowid = " + id,
                null
        );

        cursor.moveToFirst();
        int completed = cursor.getInt(0);

        cursor.close();
        db.close();

        if(completed == 1) {
            return true;
        }
        else {
            return false;
        }
    }

    public List<Integer> getUserAns(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + KEY_USER_ANS +
                        " FROM " + TABLE_WELLBEING +
                        " WHERE rowid = " + id,
                null
        );

        cursor.moveToFirst();
        String ans_str = cursor.getString(0);
        List <Integer> ans_intList = new ArrayList<>();

        if(!ans_str.equals("empty")) {
            List <String> ans_strList = Arrays.asList(ans_str.split(","));
            int ansCt = ans_strList.size();

            Log.i("DEBUG>>>> ", "ans_str = " + ans_str);
            Log.i("DEBUG>>>> ", "ans_strList = " + ans_strList.toString());
            Log.i("DEBUG>>>> ", "ansCt = " + ansCt);

            for(int j = 0; j < ansCt; j++) {
                int integer = Integer.parseInt(ans_strList.get(j));
                ans_intList.add(integer);
            }
        }
        else {
            int qCt = getQuesList(id).size();

            for(int i = 0; i < qCt; i++){
                ans_intList.add(0);
            }
        }

        cursor.close();
        db.close();

        return ans_intList;
    }

    public List< List<Integer>> getAnsVals(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + KEY_ANS_VALS +
                        " FROM " + TABLE_WELLBEING +
                        " WHERE rowid = " + id,
                null
        );

        cursor.moveToFirst();
        String    ansVal_str   = cursor.getString(0);
        String [] ansVal_array = ansVal_str.split("%nxt%");

        int ques_ct = ansVal_array.length;
        List< List<Integer> > ansVal_list = new ArrayList<>();

        for(int i = 0; i < ques_ct; i++) {
            List<Integer> ansVal_intList = new ArrayList<>();
            List<String>  ansVal_strList = Arrays.asList(ansVal_array[i].split("%%"));
            int ansCt = ansVal_strList.size();

            Log.i("DEBUG>>>>>", ansVal_strList.toString());
            for(int j = 0; j < ansCt; j++) {
                int integer = Integer.parseInt(ansVal_strList.get(j));
                ansVal_intList.add(integer);
            }

            ansVal_list.add(ansVal_intList);
        }

        cursor.close();
        db.close();

        return ansVal_list;
    }

    public List<Long> getTStamps(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + KEY_TSTAMPS +
                        " FROM " + TABLE_WELLBEING +
                        " WHERE rowid = " + id,
                null
        );

        cursor.moveToFirst();
        String tstamp_str = cursor.getString(0);
        List <Long> tstamp_longList = new ArrayList<>();

        if(!tstamp_str.equals("empty")) {
            List <String> tstamp_strList = Arrays.asList(tstamp_str.split(","));
            int tstampCt = tstamp_strList.size();

            Log.i("DEBUG>>>> ", "tstamp_str = " + tstamp_str);
            Log.i("DEBUG>>>> ", "tstamp_strList = " + tstamp_strList.toString());
            Log.i("DEBUG>>>> ", "tstampCt = " + tstampCt);

            for(int j = 0; j < tstampCt; j++) {
                long longInt = Long.parseLong(tstamp_strList.get(j));
                tstamp_longList.add(longInt);
            }
        }
        else {
            int qCt = getQuesList(id).size();

            for(int i = 0; i < qCt; i++){
                tstamp_longList.add((long) 0);
            }
        }

        cursor.close();
        db.close();

        return tstamp_longList;
    }

    public int getVersion(int id){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + KEY_VERSION +
                        " FROM " + TABLE_WELLBEING +
                        " WHERE rowid = " + id,
                null
        );

        cursor.moveToFirst();
        int version = cursor.getInt(0);

        cursor.close();
        db.close();

        return version;
    }

    public int setComplete(boolean finished, int id) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();


        values.put(KEY_COMPLETE, (finished ? 1 : 0));

        int update = db.update(TABLE_WELLBEING, values, "rowid=" + id, null);
        db.close();

        return update;
    }

    public int storeAnswers(String ans, int id) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        Log.i("STORE ANS>>>", ans);

        values.put(KEY_USER_ANS, ans);

        int update = db.update(TABLE_WELLBEING, values, "rowid=" + id, null);
        db.close();

        return update;
    }

    public int storeTStamps(String tstamps, int id) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        Log.i("STORE TSTAMPS>>>", tstamps);

        values.put(KEY_TSTAMPS, tstamps);

        int update = db.update(TABLE_WELLBEING, values, "rowid=" + id, null);
        db.close();

        return update;
    }

}

