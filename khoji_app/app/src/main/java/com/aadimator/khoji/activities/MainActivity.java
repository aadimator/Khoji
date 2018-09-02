package com.aadimator.khoji.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
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
import android.widget.Toast;

import com.aadimator.khoji.BuildConfig;
import com.aadimator.khoji.R;
import com.aadimator.khoji.fragments.AccountFragment;
import com.aadimator.khoji.fragments.ContactsFragment;
import com.aadimator.khoji.fragments.SupportMapFragment;
import com.aadimator.khoji.services.LocationJobService;
import com.aadimator.khoji.common.Constant;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity implements
        SupportMapFragment.OnFragmentInteractionListener,
        ContactsFragment.OnFragmentInteractionListener {

    private final String TAG = MainActivity.class.getSimpleName();
    private final String FRAGMENT_SAVE_KEY = "saveFragment";
    public static final String UPDATE_INTERVAL = "update_interval";

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 900000; // 15 min

    /**
     * Provides access to the Location Settings API.
     */
    private SettingsClient mSettingsClient;


    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest mLocationSettingsRequest;

    private Fragment mSelectedFragment = null;

    public static final String BUNDLE_CONTACT_DATA = "com.aadimator.khoji.activities.contactId";

    public static Intent newIntent(Context packageContext, String contactId) {
        Intent intent = new Intent(packageContext, MainActivity.class);
        intent.putExtra(BUNDLE_CONTACT_DATA, contactId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSettingsClient = LocationServices.getSettingsClient(this);

        String UID;
        if (getIntent().getExtras().getString(BUNDLE_CONTACT_DATA) != null) {
            UID = getIntent().getExtras().getString(BUNDLE_CONTACT_DATA);
        } else {
            UID = FirebaseAuth.getInstance().getUid();
        }

        setTitle(getString(R.string.app_name));

        if (savedInstanceState != null) {
            mSelectedFragment = getSupportFragmentManager().getFragment(savedInstanceState,
                    FRAGMENT_SAVE_KEY);
        } else {
            mSelectedFragment = SupportMapFragment.newInstance(UID);
        }

        changeFragment();

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        startLocationUpdates();
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_map:
                    mSelectedFragment = SupportMapFragment.newInstance(FirebaseAuth.getInstance().getUid());
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

    private void changeFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment, mSelectedFragment)
                .commit();
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (locationPermissionGiven()) {
            buildLocationSettingsRequest();
            startLocationService();
        } else {
            requestPermission();
        }
    }

    private void startLocationService() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, locationSettingsResponse -> {
                    Log.i(TAG, "All location settings are satisfied.");

                    changeFragment();

                    FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(MainActivity.this));
                    Bundle bundle = new Bundle();
                    bundle.putLong(UPDATE_INTERVAL, UPDATE_INTERVAL_IN_MILLISECONDS);

                    Job myJob = createJob(dispatcher, bundle);
                    dispatcher.mustSchedule(myJob);
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, Constant.REQUEST_CHECK_LOCATION_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private Job createJob(FirebaseJobDispatcher dispatcher, Bundle bundle) {
        return dispatcher.newJobBuilder()
                // the JobService that will be called
                .setService(LocationJobService.class)
                // uniquely identifies the job
                .setTag(LocationJobService.class.getSimpleName())
                // one-off job
                .setRecurring(false)
                // don't persist past a device reboot
                .setLifetime(Lifetime.FOREVER)
                // start between 0 and 5 seconds from now
                .setTrigger(Trigger.executionWindow(0, 5))
                // don't overwrite an existing job with the same tag
                .setReplaceCurrent(false)
                // retry with exponential backoff
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                // constraints that need to be satisfied for the job to run
                .setConstraints(
                        // run on any network
                        Constraint.ON_ANY_NETWORK
                )
                .setExtras(bundle)
                .build();
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean locationPermissionGiven() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

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
                startLocationUpdates();
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

    /**
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(new LocationRequest());
        mLocationSettingsRequest = builder.build();
    }

    /**
     * Location settings result, called from LocationHelper
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case Constant.REQUEST_CHECK_LOCATION_SETTINGS:
                switch (resultCode) {
                    case RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        startLocationUpdates();
                        break;
                    case RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        break;
                }
                break;
        }
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    public void showSnackbar(final int mainTextStringId, final int actionStringId,
                             View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getSupportFragmentManager().putFragment(
                outState,
                FRAGMENT_SAVE_KEY,
                mSelectedFragment);
    }

    @Override
    public void onContactSelection(String uid) {
        mSelectedFragment = SupportMapFragment.newInstance(uid);
        changeFragment();
//        startActivity(ChatActivity.newIntent(this, uid));
    }
}
