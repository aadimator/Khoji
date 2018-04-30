package com.aadimator.khoji.services;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.aadimator.khoji.activities.MainActivity;
import com.aadimator.khoji.models.UserLocation;
import com.aadimator.khoji.common.Constant;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class LocationJobService extends JobService {

    private static final String TAG = LocationJobService.class.getSimpleName();

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    private LocationCallback mLocationCallback;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Kick off the process of building the LocationCallback and LocationRequest.
        createLocationRequest(jobParameters);
        createLocationCallback();
        startLocationUpdates();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        stopLocationUpdates();
        return true;
    }

    /**
     * Sets up the location request.
     */
    private void createLocationRequest(JobParameters jobParameters) {
        mLocationRequest = new LocationRequest();

        long updateIntervalInMilliseconds = jobParameters.getExtras().getLong(MainActivity.UPDATE_INTERVAL);
        long fastestUpdateIntervalInMilliseconds = updateIntervalInMilliseconds / 2;

        // Sets the desired interval for active location updates. This interval is
        // inexact.
        mLocationRequest.setInterval(updateIntervalInMilliseconds);

        // Sets the fastest rate for active location updates. This interval is exact.
        mLocationRequest.setFastestInterval(fastestUpdateIntervalInMilliseconds);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Stores the current location in the FireBase DB.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                Log.i(TAG, "Working, current location: " + locationResult.getLastLocation());

                // Store the latest location in the FireBase DB
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference locations = database.getReference(Constant.FIREBASE_URL_LOCATIONS);
                locations.child(
                        FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .setValue(new UserLocation(locationResult.getLastLocation()));
            }
        };
    }


    /**
     * Requests location updates from the FusedLocationApi.
     */
    public void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback, null);
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    public void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

}
