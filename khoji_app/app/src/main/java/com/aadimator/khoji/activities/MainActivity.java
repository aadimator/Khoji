package com.aadimator.khoji.activities;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import com.aadimator.khoji.R;
import com.aadimator.khoji.fragments.MapFragment;

public class MainActivity extends AppCompatActivity implements MapFragment.OnFragmentInteractionListener {

    private TextView mTextMessage;
    private Fragment mSelectedFragment = null;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_map:
                    mSelectedFragment = MapFragment.newInstance();
                    mTextMessage.setText(R.string.title_map);
                    break;
                case R.id.navigation_contacts:
                    mTextMessage.setText(R.string.title_contacts);
                    break;
                case R.id.navigation_account:
                    mTextMessage.setText(R.string.title_account);
                    break;
            }

            changeFragment();
            return true;
        }
    };

    private void changeFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment, mSelectedFragment)
                .commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle(getString(R.string.app_name));

        mTextMessage = (TextView) findViewById(R.id.message);
        mSelectedFragment = MapFragment.newInstance();
        changeFragment();

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
