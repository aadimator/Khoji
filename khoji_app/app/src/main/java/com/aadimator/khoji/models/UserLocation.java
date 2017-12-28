package com.aadimator.khoji.models;

import android.location.Location;

import com.google.firebase.auth.FirebaseUser;

/**
 * Created by aadim on 12/26/2017.
 */

public class UserLocation {


    private long mTime;
    private float mSpeed;
    private double mAltitude;
    private double mLongitude;
    private double mLatitude;

    public UserLocation() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public UserLocation(Location location) {
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        mAltitude = location.getAltitude();
        mSpeed = location.getSpeed();
        mTime = location.getTime();
    }

    public UserLocation(long time, float speed, double altitude, double longitude, double latitude) {
        mTime = time;
        mSpeed = speed;
        mAltitude = altitude;
        mLongitude = longitude;
        mLatitude = latitude;
    }

    public long getTime() {
        return mTime;
    }

    public float getSpeed() {
        return mSpeed;
    }

    public double getAltitude() {
        return mAltitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setTime(long time) {
        mTime = time;
    }

    public void setSpeed(float speed) {
        mSpeed = speed;
    }

    public void setAltitude(double altitude) {
        mAltitude = altitude;
    }

    public void setLongitude(double longitude) {
        mLongitude = longitude;
    }

    public void setLatitude(double latitude) {
        mLatitude = latitude;
    }
}
