package com.aadimator.khoji.models;

import android.os.Parcel;
import android.os.Parcelable;

public class UserMarker implements Parcelable {

    private User mUser;
    private UserLocation mUserLocation;

    private float mDistance;
    private boolean mInRange;

    public UserMarker(User user, UserLocation userLocation) {
        mUser = user;
        mUserLocation = userLocation;
    }

    public User getUser() {
        return mUser;
    }

    public void setUser(User user) {
        mUser = user;
    }

    public UserLocation getUserLocation() {
        return mUserLocation;
    }

    public void setUserLocation(UserLocation userLocation) {
        mUserLocation = userLocation;
    }

    public float getDistance() {
        return mDistance;
    }

    public void setDistance(float distance) {
        mDistance = distance;
    }

    public boolean isInRange() {
        return mInRange;
    }

    public void setInRange(boolean inRange) {
        mInRange = inRange;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mUser, flags);
        dest.writeParcelable(this.mUserLocation, flags);
        dest.writeFloat(this.mDistance);
        dest.writeByte(this.mInRange ? (byte) 1 : (byte) 0);
    }

    protected UserMarker(Parcel in) {
        this.mUser = in.readParcelable(User.class.getClassLoader());
        this.mUserLocation = in.readParcelable(UserLocation.class.getClassLoader());
        this.mDistance = in.readFloat();
        this.mInRange = in.readByte() != 0;
    }

    public static final Parcelable.Creator<UserMarker> CREATOR = new Parcelable.Creator<UserMarker>() {
        @Override
        public UserMarker createFromParcel(Parcel source) {
            return new UserMarker(source);
        }

        @Override
        public UserMarker[] newArray(int size) {
            return new UserMarker[size];
        }
    };
}
