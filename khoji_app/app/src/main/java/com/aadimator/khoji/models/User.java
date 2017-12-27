package com.aadimator.khoji.models;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by aadim on 12/26/2017.
 */

@IgnoreExtraProperties
public class User {

    public String name;
    public String email;
    //    private String mPhoneNumber;
    public String photoUrl;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String name, String email, String photoUrl) {
        this.name = name;
        this.email = email;
        this.photoUrl = photoUrl;
    }

    public User(FirebaseUser user) {
        name = user.getDisplayName();
        email = user.getEmail();
//        mPhoneNumber = user.getPhoneNumber();
        photoUrl = (user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : "";
    }
}
