package com.aadimator.khoji.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aadimator.khoji.R;
import com.aadimator.khoji.activities.ArActivity;
import com.aadimator.khoji.activities.ChatActivity;
import com.aadimator.khoji.common.Constant;
import com.aadimator.khoji.common.GlideApp;
import com.aadimator.khoji.common.Utilities;
import com.aadimator.khoji.models.User;
import com.aadimator.khoji.models.UserLocation;
import com.aadimator.khoji.models.UserMarker;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.ar.core.ArCoreApk;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SupportMapFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SupportMapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SupportMapFragment extends Fragment implements
        OnMapReadyCallback {

    private static final String ARG_UID = "uid";
    private final String TAG = SupportMapFragment.class.getSimpleName();

    @BindView(R.id.locateInARButton)
    FloatingActionButton mArButton;

    @BindView(R.id.bs_user_info)
    LinearLayout mLayoutBottomSheet;

    @BindView(R.id.bs_user_name)
    TextView mTextViewUserName;

    @BindView(R.id.bs_user_update_time)
    TextView mTextViewUserUpdateTime;

    @BindView(R.id.bs_user_avatar)
    ImageView mImageViewUserAvatar;

    @OnClick(R.id.btn_showInAR)
    public void showInAr(View view) {
        if (!mArAvailable) {
            Toast.makeText(getContext(), "AR feature is not available.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!mSelectedUserMarker.getUserID().equals(mCurrentUser.getUid())) {
            ArrayList<UserMarker> userMarker = new ArrayList<>();
            userMarker.add(mSelectedUserMarker);
            startActivity(ArActivity.newIntent(getContext(), userMarker));
        } else {
            Toast.makeText(getContext(), "Cannot view yourself in AR.", Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.btn_sendMessage)
    public void sendMessage(View view) {
        if (!mSelectedUserMarker.getUserID().equals(mCurrentUser.getUid())) {
            startActivity(ChatActivity.newIntent(getContext(), mSelectedUserMarker.getUserID()));
        } else {
            Toast.makeText(getContext(), "Cannot send messages to yourself", Toast.LENGTH_SHORT).show();
        }
    }

    BottomSheetBehavior mSheetBehavior;

    private Activity mActivity;
    /**
     * The listener implemented by the Activity for communication with fragment.
     */
    private OnFragmentInteractionListener mListener;
    /**
     * Reference to the currently logged in user.
     */
    private FirebaseUser mCurrentUser;
    /**
     * Stores the Users in the mCurrentUser's contacts.
     */
    private HashMap<String, User> mContacts;
    /**
     * Stores the Location of the mCurrentUser's contacts.
     */
    private HashMap<String, UserLocation> mContactsLocations;
    /**
     * Represents a geographical location.
     */
    private UserLocation mCurrentLocation;
    /**
     * Is map ready
     */
    private boolean mMapReady = false;
    /**
     * Represents a map.
     */
    private GoogleMap mGoogleMap;
    /**
     * Represents the position of the current user.
     */
    private Marker mCurrentUserMarker;

    private UserMarker mSelectedUserMarker;
    /**
     * Stores the markers of all the contacts
     */
    private HashMap<String, Marker> mContactsMarkers;
    private ArrayList<UserMarker> mUserMarkers;
    /**
     * If the camera view has been updated to the user's current location.
     * Should only position once automatically.
     */
    private boolean mCameraViewUpdated = false;
    /**
     * If the user is requesting the location updates or not.
     */
    private boolean mRequestingLocationUpdates = true;

    private FusedLocationProviderClient mFusedLocationClient;

    private String mCameraFocusUid;
    private float mZoomLevel = 15.0f;
    private boolean mArAvailable = false;

    /**
     * Create a Map fragment where camera will be focused on the
     * user's location provided in the args.
     *
     * @param cameraFocusUid User ID where the Camera will be focused.
     * @return A new instance of fragment SupportMapFragment.
     */
    public static SupportMapFragment newInstance(String cameraFocusUid) {
        SupportMapFragment mapFragment = new SupportMapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_UID, cameraFocusUid);
        mapFragment.setArguments(args);
        return mapFragment;
    }

    @OnClick(R.id.locateInARButton)
    public void locateInAR(View view) {
        mUserMarkers = getUserMarkers();
        if (mUserMarkers.isEmpty()) {
            Toast.makeText(mActivity, "No friends nearby", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mArAvailable) {
            startActivity(ArActivity.newIntent(mActivity, mUserMarkers));
        } else {
            Toast.makeText(mActivity, "AR isn't supported on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    void maybeEnableArButton() {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(mActivity);
        if (availability.isTransient()) {
            // re-query at 5Hz while we check compatibility.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    maybeEnableArButton();
                }
            }, 200);
        }
        if (availability.isSupported()) {
            mArButton.setVisibility(View.VISIBLE);
            mArButton.setEnabled(true);
            mArAvailable = true;
            // indicator on the button.
        } else { // unsupported or unknown
            mArButton.setVisibility(View.INVISIBLE);
            mArButton.setEnabled(false);
            mArAvailable = false;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            mActivity = (Activity) context;
        }
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mCameraFocusUid = getArguments().getString(ARG_UID);
        }

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mContacts = new HashMap<>();
        mContactsLocations = new HashMap<>();
        mContactsMarkers = new HashMap<>();
        mUserMarkers = new ArrayList<>();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mActivity);


    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        ButterKnife.bind(this, view);

        maybeEnableArButton();

        mSheetBehavior = BottomSheetBehavior.from(mLayoutBottomSheet);
        mSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        mSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED: {
//                        btnBottomSheet.setText("Close Sheet");
                    }
                    break;
                    case BottomSheetBehavior.STATE_COLLAPSED: {
//                        btnBottomSheet.setText("Expand Sheet");
                    }
                    break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Register for the Map callback
        com.google.android.gms.maps.SupportMapFragment supportMapFragment = (com.google.android.gms.maps.SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapFragment);
        supportMapFragment.getMapAsync(this);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "OnResume()");
        // Get current user's location using last known location
        getCurrentLocation();

//        updateMap();
        startListeningForCurrentUserLocationUpdates();

        // Get the location and info of the User's contacts
        getContactsInfo();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        mActivity = null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMapReady = true;
        mGoogleMap = googleMap;

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = mGoogleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            mActivity, R.raw.dark_maps_style));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }

        mGoogleMap.setOnMarkerClickListener(marker -> {
            Log.d(TAG, "Marker clicked: " + marker.getTitle());
//                mUserMarkers = getUserMarkers();
//                startActivity(ArActivity.newIntent(mActivity, mUserMarkers));
            UserMarker userMarker = (UserMarker) marker.getTag();
            assert userMarker != null;
            mSelectedUserMarker = userMarker;
            updateBottomSheet();
            if (mSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                mSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
//                } else {
//                    mSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
//                }
            return true;
        });
        updateMap();
    }

    private void updateBottomSheet() {
        mTextViewUserName.setText(mSelectedUserMarker.getUser().getName());
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getContext());
        DateFormat dateFormat = android.text.format.DateFormat.getLongDateFormat(getContext());
        Long time = mSelectedUserMarker.getUserLocation().getTime();
        mTextViewUserUpdateTime.setText(String.format("%s %s", dateFormat.format(time), timeFormat.format(time)));

        Drawable placeholder = Utilities.getTinted
                (
                        mActivity,
                        R.drawable.user_avatar,
                        mActivity.getResources().getColor(R.color.flatHalfWhite)
                );

        GlideApp.with(this)
                .load(mSelectedUserMarker.getUser().getPhotoUrl())
                .placeholder(placeholder)
                .circleCrop()
                .into(mImageViewUserAvatar);
    }

    private ArrayList<UserMarker> getUserMarkers() {
        ArrayList<UserMarker> userMarkers = new ArrayList<>();
        for (String key : mContacts.keySet()) {
            userMarkers.add(new UserMarker(key, mContacts.get(key), mContactsLocations.get(key)));
        }
        return userMarkers;
    }

    private void updateCameraView(float zoomLevel) {
        if (!mCameraViewUpdated) {
            LatLng latLng;
            if (mCameraFocusUid.equals(mCurrentUser.getUid())) {
                latLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            } else {
                if (!mContactsLocations.isEmpty() &&
                        mContactsLocations.containsKey(mCameraFocusUid)) {
                    UserLocation location = mContactsLocations.get(mCameraFocusUid);
                    latLng = new LatLng(location.getLatitude(), location.getLongitude());
                } else {
                    return;
                }
            }
            mGoogleMap.animateCamera(CameraUpdateFactory
                    .newLatLngZoom(latLng, zoomLevel));
            mCameraViewUpdated = true;
        }
    }

    /**
     * Update the map to show user's and contacts markers
     */
    private void updateMap() {
        if (mMapReady && mGoogleMap != null) {
            if (mCurrentLocation != null) {
                updateCurrentUserMarker();
                updateContactsMarkers();
                updateCameraView(mZoomLevel);
            }
        }
    }

    /**
     * Add the current User's marker. We don't want this and contacts markers
     * to be the same (diff icons etc).
     * That's why writing this in a separate function.
     */
    private void updateCurrentUserMarker() {
        if (mCurrentLocation == null) return;

        LatLng latLng = new LatLng(
                mCurrentLocation.getLatitude(),
                mCurrentLocation.getLongitude()
        );
        if (mCurrentUserMarker == null) {
            mCurrentUserMarker = mGoogleMap
                    .addMarker(new MarkerOptions()
                            .position(latLng)
                            .title(mCurrentUser.getDisplayName())
                            .icon(BitmapDescriptorFactory
                                    .defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    );
            mCurrentUserMarker.showInfoWindow();
            mCurrentUserMarker.setTag(
                    new UserMarker(
                            mCurrentUser.getUid(),
                            new User(
                                    mCurrentUser.getDisplayName(),
                                    mCurrentUser.getEmail(),
                                    mCurrentUser.getPhotoUrl() == null ? "" : mCurrentUser.getPhotoUrl().toString()
                            ),
                            mCurrentLocation)
            );
        } else {
            mCurrentUserMarker.setPosition(latLng);
        }
    }

    /**
     * Add the Contact markers on the map.
     */
    private void updateContactsMarkers() {
        for (Map.Entry<String, UserLocation> entry : mContactsLocations.entrySet()) {
            UserLocation location = entry.getValue();
            User user = mContacts.get(entry.getKey());
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (mContactsMarkers.containsKey(entry.getKey())) {
                Marker marker = mContactsMarkers.get(entry.getKey());
                marker.setPosition(latLng);
            } else {
                Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(user.getName()));
                marker.setTag(new UserMarker(entry.getKey(), user, location));
                mContactsMarkers.put(entry.getKey(), marker);
            }
        }
    }

    /**
     * Get the Contacts Info from the Firebase DB
     */
    private void getContactsInfo() {
        final FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseDatabase.getReference(Constant.FIREBASE_URL_CONTACTS)
                .child(mCurrentUser.getUid())
                .orderByKey()
                .addValueEventListener(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                // Get the Contacts from the dataSnapshot
                                getUsers(dataSnapshot);

                                // Get the locations from the dataSnapshot
                                getLocations(dataSnapshot);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        }
                );
    }

    /**
     * Retrieve the Logged In User's current location from the Firebase DB
     */
    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        if (locationPermissionGiven()) {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(mActivity, location -> {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            mCurrentLocation = new UserLocation(location);

                            // Store the latest location in the FireBase DB
                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            DatabaseReference locations = database.getReference(Constant.FIREBASE_URL_LOCATIONS);
                            locations.child(mCurrentUser.getUid()).setValue(new UserLocation(location));

                            updateMap();
                        } else {
                            startListeningForCurrentUserLocationUpdates();
                        }
                    });
        }
    }

    private void startListeningForCurrentUserLocationUpdates() {
        FirebaseDatabase.getInstance()
                .getReference(Constant.FIREBASE_URL_LOCATIONS)
                .child(mCurrentUser.getUid())
                .addValueEventListener(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                UserLocation location = dataSnapshot.getValue(UserLocation.class);
                                assert location != null;
                                if (mCurrentLocation != null && location.getTime() > mCurrentLocation.getTime()) {
                                    mCurrentLocation = location;
                                    updateMap();
                                }

                                if (mCurrentLocation == null) {
                                    mCurrentLocation = location;
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        }
                );
    }

    /**
     * Retrieve the User given a dataSnapshot of the UIDs and store them in the mContacts
     */
    private void getUsers(DataSnapshot dataSnapshot) {
        for (DataSnapshot child : dataSnapshot.getChildren()) {
            FirebaseDatabase.getInstance()
                    .getReference(Constant.FIREBASE_URL_USERS)
                    .child(child.getKey())
                    .orderByKey()
                    .addValueEventListener(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (!dataSnapshot.getKey().equals(Constant.BOT_UID)) {
                                        User user = dataSnapshot.getValue(User.class);
                                        mContacts.put(dataSnapshot.getKey(), user);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            }
                    );
        }
    }

    /**
     * Retrieve the Locations of the users given a dataSnapshot of the UIDs and
     * store them in the mContactsLocations
     */
    private void getLocations(DataSnapshot dataSnapshot) {
        for (DataSnapshot child : dataSnapshot.getChildren()) {
            FirebaseDatabase.getInstance()
                    .getReference(Constant.FIREBASE_URL_LOCATIONS)
                    .child(child.getKey())
                    .orderByKey()
                    .addValueEventListener(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (!dataSnapshot.getKey().equals(Constant.BOT_UID)) {
                                        UserLocation location = dataSnapshot.getValue(UserLocation.class);
                                        mContactsLocations.put(dataSnapshot.getKey(), location);
                                        updateMap();
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            }
                    );
        }
    }

    private boolean locationPermissionGiven() {
        int permissionState = ActivityCompat.checkSelfPermission(mActivity,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void requestPermission();
    }
}
