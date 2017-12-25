package com.aadimator.khoji.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import com.aadimator.khoji.Constant;
import com.aadimator.khoji.R;
import com.aadimator.khoji.fragments.AccountFragment;
import com.aadimator.khoji.fragments.ContactsFragment;
import com.aadimator.khoji.fragments.MapFragment;

public class MainActivity extends AppCompatActivity implements
        MapFragment.OnFragmentInteractionListener,
        ContactsFragment.OnFragmentInteractionListener,
        AccountFragment.OnFragmentInteractionListener {

    private Fragment mSelectedFragment = null;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_map:
                    mSelectedFragment = MapFragment.newInstance();
                    break;
                case R.id.navigation_contacts:
                    mSelectedFragment = ContactsFragment.newInstance("a", "b");
                    break;
                case R.id.navigation_account:
                    mSelectedFragment = AccountFragment.newInstance("c", "d");
                    break;
            }

            changeFragment();
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle(getString(R.string.app_name));

        mSelectedFragment = MapFragment.newInstance();
        changeFragment();

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }


    /**
     * startResolutionForResult in the MapFragment isn't calling Fragment's
     * onActivityResult but MainActivity's, so this is a hack to call
     * Fragment's onActivityResult.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mSelectedFragment.onActivityResult(requestCode, resultCode, data);
    }

    private void changeFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment, mSelectedFragment)
                .commit();
    }


    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
