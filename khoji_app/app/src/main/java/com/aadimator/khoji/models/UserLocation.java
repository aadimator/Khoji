package com.aadimator.khoji.models;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.Exclude;

/**
 * Created by aadim on 12/26/2017.
 */

public class UserLocation implements Parcelable {


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

    @Exclude
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(this.mLatitude);
        dest.writeDouble(this.mLongitude);
        dest.writeDouble(this.mAltitude);
        dest.writeFloat(this.mSpeed);
        dest.writeFloat(this.mAccuracy);
        dest.writeLong(this.mTime);
    }

    protected UserLocation(Parcel in) {
        this.mLatitude = in.readDouble();
        this.mLongitude = in.readDouble();
        this.mAltitude = in.readDouble();
        this.mSpeed = in.readFloat();
        this.mAccuracy = in.readFloat();
        this.mTime = in.readLong();
    }

    public static final Parcelable.Creator<UserLocation> CREATOR = new Parcelable.Creator<UserLocation>() {
        @Override
        public UserLocation createFromParcel(Parcel source) {
            return new UserLocation(source);
        }

        @Override
        public UserLocation[] newArray(int size) {
            return new UserLocation[size];
        }
    };
}
