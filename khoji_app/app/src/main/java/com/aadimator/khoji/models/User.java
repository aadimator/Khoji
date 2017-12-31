package com.aadimator.khoji.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by aadim on 12/26/2017.
 */

@IgnoreExtraProperties
public class User implements Parcelable {

    private String mName;
    private String mEmail;
    private String mPhotoUrl;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String name, String email, String photoUrl) {
        this.mName = name;
        this.mEmail = email;
        this.mPhotoUrl = photoUrl;
    }

    public User(FirebaseUser user) {
        mName = user.getDisplayName();
        mEmail = user.getEmail();
        mPhotoUrl = (user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : "";
    }

    protected User(Parcel in) {
        mName = in.readString();
        mEmail = in.readString();
        mPhotoUrl = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String email) {
        this.mEmail = email;
    }

    public String getPhotoUrl() {
        return mPhotoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.mPhotoUrl = photoUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mName);
        parcel.writeString(mEmail);
        parcel.writeString(mPhotoUrl);
    }
}
