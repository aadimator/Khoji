package com.aadimator.khoji.models;

public class UserMarker {

    private User mUser;
    private UserLocation mUserLocation;

    private float mDistance;
    private boolean mInRange;
    private float[] mZeroMatrix;

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

    public float[] getZeroMatrix() {
        return mZeroMatrix;
    }

    public void setZeroMatrix(float[] zeroMatrix) {
        mZeroMatrix = zeroMatrix;
    }
}
