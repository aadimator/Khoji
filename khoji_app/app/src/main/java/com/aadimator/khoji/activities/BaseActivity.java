package com.aadimator.khoji.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.aadimator.khoji.R;
import com.aadimator.khoji.models.User;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class BaseActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            // already signed in
            updateDB(auth.getCurrentUser());
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            // TODO Add on-boarding screens later
        }
    }

    private void updateDB(FirebaseUser user) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference users = database.getReference("users");
        users.child(user.getUid()).setValue(new User(user));
    }

}
