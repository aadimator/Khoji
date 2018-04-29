package com.aadimator.khoji.models;

import android.location.Location;

import com.google.firebase.auth.FirebaseUser;

/**
 * Created by aadim on 12/26/2017.
 */

public class UserLocation {


    private double mLatitude;
    private double mLongitude;
    private double mAltitude;
    private float mSpeed;
    private float mAccuracy;
    private long mTime;

    public UserLocation() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public UserLocation(Location location) {
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        mAltitude = location.getAltitude();
        mSpeed = location.getSpeed();
        mTime = location.getTime();
        mAccuracy = location.getAccuracy();
    }

    public UserLocation(double latitude, double longitude) {
        mLatitude = latitude;
        mLongitude = longitude;
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

    public float getAccuracy() {
        return mAccuracy;
    }

    public void setAccuracy(float accuracy) {
        mAccuracy = accuracy;
    }

    public Location getLocation() {
        Location location = new Location(UserLocation.class.getSimpleName());
        location.setLatitude(mLatitude);
        location.setLongitude(mLongitude);
        location.setAltitude(mAltitude);
        location.setSpeed(mSpeed);
        location.setTime(mTime);
        location.setAccuracy(mAccuracy);
        return location;
    }
}
