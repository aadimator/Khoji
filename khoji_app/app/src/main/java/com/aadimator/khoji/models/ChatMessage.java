package com.aadimator.khoji.models;

import java.util.Date;

public class ChatMessage {

    private String mText;
    private String mUserId;
    private String mUserName;
    private long mTime;

    public ChatMessage(String text, String userId, String userName) {
        this.mText = text;
        this.mUserId = userId;
        this.mUserName = userName;

        // Initialize to current time
        mTime = new Date().getTime();
    }

    public ChatMessage() {

    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
    }

    public String getUserId() {
        return mUserId;
    }

    public void setUserId(String userId) {
        mUserId = userId;
    }

    public String getUserName() {
        return mUserName;
    }

    public void setUserName(String userName) {
        mUserName = userName;
    }

    public long getTime() {
        return mTime;
    }

    public void setTime(long time) {
        mTime = time;
    }
}