package com.aadimator.khoji.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aadimator.khoji.R;
import com.aadimator.khoji.models.User;
import com.aadimator.khoji.models.UserLocation;
import com.aadimator.khoji.utils.Constant;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MapFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFragment extends Fragment implements
        OnMapReadyCallback {

    private final String TAG = MapFragment.class.getSimpleName();
    private Activity mActivity;

    private static final String ARG_UID = "uid";


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

    /**
     * Stores the markers of all the contacts
     */
    private HashMap<String, Marker> mContactsMarkers;

    /**
     * If the camera view has been updated to the user's current location.
     * Should only position once automatically.
     */
    private boolean mCameraViewUpdated = false;

    /**
     * If the user is requesting the location updates or not.
     */
    private boolean mRequestingLocationUpdates = true;

    private String mCameraFocusUid;
    private float mZoomLevel = 15.0f;


    /**
     * Create a Map fragment where camera will be focused on the
     * user's location provided in the args.
     *
     * @param cameraFocusUid User ID where the Camera will be focused.
     * @return A new instance of fragment MapFragment.
     */
    public static MapFragment newInstance(String cameraFocusUid) {
        MapFragment mapFragment = new MapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_UID, cameraFocusUid);
        mapFragment.setArguments(args);
        return mapFragment;
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

        // Get current's user's location from Firebase DB
        getCurrentLocation();

        // Get the location and info of the User's contacts
        getContactsInfo();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Register for the Map callback
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(this);
    }

    @Override
    public void onStart() {
        super.onStart();
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
        updateMap();
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
    private void getCurrentLocation() {
        FirebaseDatabase.getInstance()
                .getReference(Constant.FIREBASE_URL_LOCATIONS)
                .child(mCurrentUser.getUid())
                .addValueEventListener(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                mCurrentLocation = dataSnapshot.getValue(UserLocation.class);
                                updateMap();
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
                                    User user = dataSnapshot.getValue(User.class);
                                    mContacts.put(dataSnapshot.getKey(), user);
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
                                    UserLocation location = dataSnapshot.getValue(UserLocation.class);
                                    mContactsLocations.put(dataSnapshot.getKey(), location);
                                    updateMap();
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            }
                    );
        }
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
