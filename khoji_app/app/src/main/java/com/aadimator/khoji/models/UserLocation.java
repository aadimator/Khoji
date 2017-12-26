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
    private double mLogitude;
    private double mLatitude;

    public UserLocation() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public UserLocation(Location location) {
        mLatitude = location.getLatitude();
        mLogitude = location.getLongitude();
        mAltitude = location.getAltitude();
        mSpeed = location.getSpeed();
        mTime = location.getTime();
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

    public double getLogitude() {
        return mLogitude;
    }

    public double getLatitude() {
        return mLatitude;
    }
}
