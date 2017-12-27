package com.aadimator.khoji.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.aadimator.khoji.BuildConfig;
import com.aadimator.khoji.utils.Constant;
import com.aadimator.khoji.R;
import com.aadimator.khoji.fragments.AccountFragment;
import com.aadimator.khoji.fragments.ContactsFragment;
import com.aadimator.khoji.fragments.MapFragment;

public class MainActivity extends AppCompatActivity implements
        MapFragment.OnFragmentInteractionListener,
        ContactsFragment.OnFragmentInteractionListener,
        AccountFragment.OnFragmentInteractionListener {

    private final String TAG = MainActivity.class.getSimpleName();
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
                    mSelectedFragment = ContactsFragment.newInstance();
                    break;
                case R.id.navigation_account:
                    mSelectedFragment = AccountFragment.newInstance();
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


    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == Constant.MY_PERMISSIONS_REQUEST_FINE_LOCATION) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted, updates requested, starting location updates");
                ((MapFragment) mSelectedFragment).startLocationUpdates();
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    private void changeFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment, mSelectedFragment)
                .commit();
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }


    @Override
    public void requestPermission() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    Constant.MY_PERMISSIONS_REQUEST_FINE_LOCATION);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Constant.MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
