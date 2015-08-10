package com.example.natalie.android_wellbeing;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by Natalie on 6/28/2015.
 */

@ParseClassName("SurveyResponseBundle")
public class SurveyResponseBundle extends ParseObject {
    public String getAppID() {
        return getString("appID");
    }

    public void setAppID(String appID) {
        put("appID", appID);
    }

    public String getUserEmail() {
        return getString("userEmail");
    }

    public void setUserEmail(String userEmail) {
        put("userEmail", userEmail);
    }

    public String getUserID() {
        return getString("userID");
    }

    public void setUserID(String userID) {
        put("userID", userID);
    }

    public int getSurveyID() {
        return getInt("surveyID");
    }

    public void setSurveyID(int surveyID) {
        put("surveyID", surveyID);
    }

    public String getDeviceID() {
        return getString("deviceID");
    }

    public void setDeviceIDID(String deviceID) {
        put("deviceID", deviceID);
    }

    public int getQuestionID() {
        return getInt("questionID");
    }

    public void setQuestionID(int questionID) {
        put("questionID", questionID);
    }

    public int getUnixTimeStamp() {
        return getInt("unixTimeStamp");
    }

    public void setUnixTimeStamp(int unixTimeStamp) {
        put("unixTimeStamp", unixTimeStamp);
    }

    public List<Long> getQuestionResponse() {
        List<Object> tstamps = getList("unixTimeStamp");
        List<Long> long_tstamps = new ArrayList<>();

        for(Object time : tstamps){
            long_tstamps.add((long) time);
        }

        return long_tstamps;
    }

    public void setQuestionResponse(int questionResponse) {
        put("questionResponse", questionResponse);
    }

    public static ParseQuery<SurveyResponseBundle> getQuery() {
        return ParseQuery.getQuery(SurveyResponseBundle.class);
    }
}
