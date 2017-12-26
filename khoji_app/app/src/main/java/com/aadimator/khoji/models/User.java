package com.aadimator.khoji.models;

import android.net.Uri;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Created by aadim on 12/26/2017.
 */

public class User {

    private String mName;
    private String mEmail;
    private String mPhoneNumber;
    private String mPhotoUrl;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(FirebaseUser user) {
        mName = user.getDisplayName();
        mEmail = user.getEmail();
        mPhoneNumber = user.getPhoneNumber();
        mPhotoUrl = user.getPhotoUrl().toString();
    }

    public String getName() {
        return mName;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    //
    public String getPhotoUrl() {
        return mPhotoUrl;
    }
}
