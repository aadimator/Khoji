package com.aadimator.khoji.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.aadimator.khoji.R;
import com.aadimator.khoji.models.User;
import com.aadimator.khoji.common.Constant;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class BaseActivity extends AppCompatActivity {

    static {
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            // already signed in
            updateDB(auth.getCurrentUser());
            addBotContact(auth.getCurrentUser());
            startActivity(MainActivity.newIntent(this, auth.getUid()));
            finish();
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            // TODO Add on-boarding screens later
        }
    }

    private void updateDB(FirebaseUser user) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference users = database.getReference(Constant.FIREBASE_URL_USERS);
        users.child(user.getUid()).setValue(new User(user));
    }

    private void addBotContact(FirebaseUser user) {
        DatabaseReference contacts = FirebaseDatabase.getInstance().getReference(Constant.FIREBASE_URL_CONTACTS);
        contacts.child(user.getUid())
                .child(Constant.BOT_UID)
                .setValue(Constant.BOT_NAME);
        contacts.child(Constant.BOT_UID)
                .child(user.getUid())
                .setValue(user.getDisplayName());
    }

}
