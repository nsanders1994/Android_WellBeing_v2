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

public class TEMP extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION   = 4;
    private static final String DATABASE_NAME   = "WellbeingManager";
    private static final String TABLE_WELLBEING = "Wellbeing";
    private static final String KEY_POPUP_HR    = "PopupHr";
    private static final String KEY_POPUP_MIN   = "PopupMin";
    private static final String KEY_DURATION    = "Duration";
    private static final String KEY_NAME        = "Name";
    private static final String KEY_QUES        = "Questions";
    private static final String KEY_ANSRS       = "Answers";
    private static final String KEY_TYPES       = "Types";

    public TEMP(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_WELLBEING + "("
                + KEY_POPUP_HR    + " INTEGER,"
                + KEY_POPUP_MIN   + " INTEGER,"
                + KEY_DURATION    + " INTEGER,"
                + KEY_NAME        + " STRING,"
                + KEY_QUES        + " STRING,"
                + KEY_ANSRS       + " STRING,"
                + KEY_TYPES       + " STRING)");
    }

    public void createSurvey(int hr, int min, int dur, String name, String ques, String ans, String types) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_POPUP_HR,    hr);
        values.put(KEY_POPUP_MIN,   min);
        values.put(KEY_DURATION,    dur);
        values.put(KEY_NAME,        name);
        values.put(KEY_QUES,        ques);
        values.put(KEY_ANSRS,       ans);
        values.put(KEY_TYPES,       types);

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

    public int getHr(int id){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + KEY_POPUP_HR +
                        " FROM " + TABLE_WELLBEING +
                        " WHERE rowid = " + id,
                null
        );

        cursor.moveToFirst();
        int hr = cursor.getInt(0); //Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_POPUP_HR)));

        cursor.close();
        db.close();

        return hr;
    }

    public int getMin(int id){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + KEY_POPUP_MIN +
                        " FROM " + TABLE_WELLBEING +
                        " WHERE rowid = " + id,
                null
        );

        cursor.moveToFirst();
        int min = cursor.getInt(0); //Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_POPUP_MIN)));

        cursor.close();
        db.close();

        return min;
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
}

