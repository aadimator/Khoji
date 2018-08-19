package com.aadimator.khoji.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aadimator.khoji.R;
import com.aadimator.khoji.common.Constant;
import com.aadimator.khoji.common.DemoUtils;
import com.aadimator.khoji.common.GlideApp;
import com.aadimator.khoji.common.Utilities;
import com.aadimator.khoji.models.UserLocation;
import com.aadimator.khoji.models.UserMarker;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import uk.co.appoly.arcorelocation.LocationScene;
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore and Sceneform APIs.
 */
public class ArActivity extends AppCompatActivity implements UserMarker.OnMarkerTouchListener {

    public static final String BUNDLE_MARKERS_LIST = "com.aadimator.khoji.activities.markersList";
    public static final String BUNDLE_CONTACT_ID_LIST = "com.aadimator.khoji.activities.contactIdsList";
    private static final String TAG = ArActivity.class.getSimpleName();

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] mAnchorMatrix = new float[16];
    // Anchors created from taps used for object placing.
    private final ArrayList<Anchor> mAnchors = new ArrayList<>();
    private boolean installRequested;
    private boolean hasFinishedLoading = false;
    private Snackbar loadingMessageSnackbar = null;
    private ArSceneView arSceneView;
    // Renderables for this example
    private ModelRenderable andyRenderable;
    private ViewRenderable exampleLayoutRenderable;
    // Our ARCore-Location scene
    private LocationScene locationScene;
    private ArrayList<UserMarker> mMarkerList;
    private ArrayList<String> mContactIds = new ArrayList<>();

    @BindView(R.id.bs_user_info)
    LinearLayout mLayoutBottomSheet;

    @BindView(R.id.bs_user_name)
    TextView mTextViewUserName;

    @BindView(R.id.bs_user_update_time)
    TextView mTextViewUserUpdateTime;

    @BindView(R.id.bs_user_avatar)
    ImageView mImageViewUserAvatar;

    @BindView(R.id.btn_showInAR)
    ImageButton mButtonShowInAR;

    @BindView(R.id.btn_sendMessage)
    ImageButton mButtonSendMessage;

    BottomSheetBehavior mSheetBehavior;


    public static Intent newIntent(Context packageContext,
                                   ArrayList<UserMarker> userMarkers) {
        Intent intent = new Intent(packageContext, ArActivity.class);
        intent.putExtra(BUNDLE_MARKERS_LIST, userMarkers);
        return intent;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);

        ButterKnife.bind(this);

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

        arSceneView = findViewById(R.id.ar_scene_view);

        mMarkerList = new ArrayList<>();
        if (Objects.requireNonNull(getIntent().getExtras())
                .get(BUNDLE_MARKERS_LIST) != null) {
            mMarkerList = (ArrayList<UserMarker>) getIntent()
                    .getExtras().getSerializable(BUNDLE_MARKERS_LIST);
        }


        // Build a renderable from a 2D View.
        CompletableFuture<ViewRenderable> exampleLayout =
                ViewRenderable.builder()
                        .setView(this, R.layout.ar_marker_layout)
                        .build();

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
//        CompletableFuture<ModelRenderable> andy = ModelRenderable.builder()
//                .setSource(this, R.raw.andy)
//                .build();


        CompletableFuture.allOf(
                exampleLayout)
//                ,
//                andy)
                .handle(
                        (notUsed, throwable) -> {
                            // When you build a Renderable, Sceneform loads its resources in the background while
                            // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                            // before calling get().

                            if (throwable != null) {
                                DemoUtils.displayError(this, "Unable to load renderables", throwable);
                                return null;
                            }

                            try {
                                exampleLayoutRenderable = exampleLayout.get();
//                                andyRenderable = andy.get();
                                hasFinishedLoading = true;

                            } catch (InterruptedException | ExecutionException ex) {
                                DemoUtils.displayError(this, "Unable to load renderables", ex);
                            }

                            return null;
                        });

        // Set an update listener on the Scene that will hide the loading message once a Plane is
        // detected.
        arSceneView
                .getScene()
                .setOnUpdateListener(
                        frameTime -> {
                            if (!hasFinishedLoading) {
                                return;
                            }

                            if (locationScene == null) {
                                // If our locationScene object hasn't been setup yet, this is a good time to do it
                                // We know that here, the AR components have been initiated.
                                locationScene = new LocationScene(this, this, arSceneView);
                                // stop trying to refine the angles so often
                                locationScene.setMinimalRefreshing(true);
                                // refresh only when it detects a location change from the device
                                locationScene.setRefreshAnchorsAsLocationChanges(true);
//                                locationScene.setAnchorRefreshInterval(60);
                                locationScene.setOffsetOverlapping(true);

                                // Now lets create our location markers.
                                for (UserMarker marker : mMarkerList) {
                                    marker.render(this, locationScene, ArActivity.this);
                                    mContactIds.add(marker.getUserID());
                                }
                            }

                            Frame frame = arSceneView.getArFrame();
                            if (frame == null) {
                                return;
                            }

                            if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                                return;
                            }

                            if (locationScene != null) {
                                locationScene.processFrame(frame);
                            }

                            if (loadingMessageSnackbar != null) {
                                for (Plane plane : frame.getUpdatedTrackables(Plane.class)) {
                                    if (plane.getTrackingState() == TrackingState.TRACKING) {
                                        hideLoadingMessage();
                                    }
                                }
                            }
                        });

        listenForUpdates();


        // Lastly request CAMERA & fine location permission which is required by ARCore-Location.
        ARLocationPermissionHelper.requestPermission(this);
    }

    private void listenForUpdates() {
        for (UserMarker marker : mMarkerList) {
            String key = marker.getUserID();
            FirebaseDatabase.getInstance()
                    .getReference(Constant.FIREBASE_URL_LOCATIONS)
                    .child(key)
                    .orderByKey()
                    .addValueEventListener(
                            new ValueEventListener() {
                                @RequiresApi(api = Build.VERSION_CODES.N)
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    UserLocation location = dataSnapshot.getValue(UserLocation.class);
                                    UserMarker userMarker = mMarkerList.get(mMarkerList.indexOf(marker));
                                    // if location changed by 1 meter.
                                    if (userMarker.locationChangedBy(location, 1)) {
                                        userMarker.setUserLocation(location);
                                        mMarkerList.set(mMarkerList.indexOf(marker), userMarker);
                                        if (!mContactIds.isEmpty() && locationScene != null && !locationScene.mLocationMarkers.isEmpty()) {
                                            int markerIndex = mContactIds.indexOf(userMarker.getUserID());
                                            if (markerIndex == -1 || markerIndex >= locationScene.mLocationMarkers.size())
                                                return;
                                            locationScene.mLocationMarkers.get(markerIndex).anchorNode.getAnchor().detach();
                                            locationScene.mLocationMarkers.get(markerIndex).anchorNode.setAnchor(null);
                                            locationScene.mLocationMarkers.get(markerIndex).anchorNode.setEnabled(false);
                                            locationScene.mLocationMarkers.get(markerIndex).anchorNode = null;
                                            locationScene.mLocationMarkers.remove(markerIndex);
                                            userMarker.render(getApplicationContext(), locationScene, ArActivity.this);

                                            // Remove the current user from ContactIds so indexes don't clash
                                            mContactIds.remove(markerIndex);
                                            mContactIds.add(userMarker.getUserID());
//                                        locationScene.refreshAnchors();
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            }
                    );
        }
    }

//    private void updateMarkers() {
//        for (Map.Entry<String, UserMarker> entry : mMarkerList.entrySet()) {
//            UserMarker marker = entry.getValue();
//            LocationMarker locationMarker = new LocationMarker(
//                    marker.getUserLocation().getLongitude(),
//                    marker.getUserLocation().getLatitude(),
//                    new AnnotationRenderer(marker.getUser().getName())
//            );
//            locationMarker.setOnTouchListener(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(ArActivity.this,
//                            "Touched Aadam", Toast.LENGTH_SHORT).show();
//                }
//            });
//
//            mLocationScene.mLocationMarkers.add(locationMarker);
//        }
//    }

    /**
     * Example node of a layout
     *
     * @return Node base.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private Node getExampleView() {
        Node base = new Node();
        base.setRenderable(exampleLayoutRenderable);
        Context c = this;
        // Add  listeners etc here
        View eView = exampleLayoutRenderable.getView();
        eView.setOnTouchListener((v, event) -> {
            Toast.makeText(
                    c, "Location marker touched.", Toast.LENGTH_LONG)
                    .show();
            return false;
        });

        return base;
    }

    /***
     * Example Node of a 3D model
     *
     * @return Node base.
     */
    private Node getAndy() {
        Node base = new Node();
        base.setRenderable(andyRenderable);
        Context c = this;
        base.setOnTapListener((v, event) -> {
            Toast.makeText(
                    c, "Andy touched.", Toast.LENGTH_LONG)
                    .show();
        });
        return base;
    }

    /**
     * Make sure we call locationScene.resume();
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (locationScene != null) {
            locationScene.resume();
        }

        if (arSceneView.getSession() == null) {
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                Session session = DemoUtils.createArSession(this, installRequested);
                if (session == null) {
                    installRequested = ARLocationPermissionHelper.hasPermission(this);
                    return;
                } else {
                    arSceneView.setupSession(session);
                }
            } catch (UnavailableException e) {
                DemoUtils.handleSessionException(this, e);
            }
        }

        try {
            arSceneView.resume();
        } catch (CameraNotAvailableException ex) {
            DemoUtils.displayError(this, "Unable to get camera", ex);
            finish();
            return;
        }

        if (arSceneView.getSession() != null) {
            showLoadingMessage();
        }
    }

    /**
     * Make sure we call locationScene.pause();
     */
    @Override
    public void onPause() {
        super.onPause();

        if (locationScene != null) {
            locationScene.pause();
        }

        arSceneView.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        arSceneView.destroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        if (!ARLocationPermissionHelper.hasPermission(this)) {
            if (!ARLocationPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                ARLocationPermissionHelper.launchPermissionSettings(this);
            } else {
                Toast.makeText(
                        this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                        .show();
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void showLoadingMessage() {
        if (loadingMessageSnackbar != null && loadingMessageSnackbar.isShownOrQueued()) {
            return;
        }

        loadingMessageSnackbar =
                Snackbar.make(
                        ArActivity.this.findViewById(android.R.id.content),
                        R.string.plane_finding,
                        Snackbar.LENGTH_INDEFINITE);
        loadingMessageSnackbar.getView().setBackgroundColor(0xbf323232);
        loadingMessageSnackbar.show();
    }

    private void hideLoadingMessage() {
        if (loadingMessageSnackbar == null) {
            return;
        }

        loadingMessageSnackbar.dismiss();
        loadingMessageSnackbar = null;
    }

    @Override
    public void onMarkerTouched(UserMarker marker) {
        // update the bottom sheet
        mTextViewUserName.setText(marker.getUser().getName());
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(this);
        DateFormat dateFormat = android.text.format.DateFormat.getLongDateFormat(this);
        Long time = marker.getUserLocation().getTime();
        mTextViewUserUpdateTime.setText(String.format("%s %s", dateFormat.format(time), timeFormat.format(time)));

        Drawable placeholder = Utilities.getTinted
                (
                        this,
                        R.drawable.user_avatar,
                        this.getResources().getColor(R.color.flatHalfWhite)
                );

        GlideApp.with(this)
                .load(marker.getUser().getPhotoUrl())
                .placeholder(placeholder)
                .circleCrop()
                .into(mImageViewUserAvatar);

        mButtonShowInAR.setVisibility(View.GONE);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mButtonSendMessage.getLayoutParams();
        layoutParams.weight = 2;
        mButtonSendMessage.setLayoutParams(layoutParams);
        mButtonSendMessage.setOnClickListener(v -> startActivity(ChatActivity.newIntent(ArActivity.this, marker.getUserID())));

        mSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}