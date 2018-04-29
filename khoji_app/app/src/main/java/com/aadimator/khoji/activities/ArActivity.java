package com.aadimator.khoji.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.aadimator.khoji.R;
import com.aadimator.khoji.common.helpers.CameraPermissionHelper;
import com.aadimator.khoji.common.helpers.DisplayRotationHelper;
import com.aadimator.khoji.common.helpers.FullScreenHelper;
import com.aadimator.khoji.common.helpers.SnackbarHelper;
import com.aadimator.khoji.common.helpers.TapHelper;
import com.aadimator.khoji.common.rendering.BackgroundRenderer;
import com.aadimator.khoji.common.rendering.ObjectRenderer;
import com.aadimator.khoji.models.User;
import com.aadimator.khoji.models.UserLocation;
import com.aadimator.khoji.models.UserMarker;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;
import uk.co.appoly.arcorelocation.rendering.AnnotationRenderer;

public class ArActivity extends AppCompatActivity implements GLSurfaceView.Renderer {

    private static final String TAG = ArActivity.class.getSimpleName();

    public static final String BUNDLE_MARKERS_LIST = "com.aadimator.khoji.activities.markersList";

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView mSurfaceView;

    private boolean mInstallRequested;

    private Session mSession;
    private final SnackbarHelper mMessageSnackbarHelper = new SnackbarHelper();
    private DisplayRotationHelper mDisplayRotationHelper;
    private TapHelper mTapHelper;

    private final BackgroundRenderer mBackgroundRenderer = new BackgroundRenderer();
    private final ObjectRenderer mVirtualObject = new ObjectRenderer();
    private final ObjectRenderer mVirtualObjectShadow = new ObjectRenderer();
    //    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();
//    private final PlaneRenderer planeRenderer = new PlaneRenderer();

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] mAnchorMatrix = new float[16];

    // Anchors created from taps used for object placing.
    private final ArrayList<Anchor> mAnchors = new ArrayList<>();

    private LocationScene mLocationScene;


    private ArrayList<UserMarker> mMarkerList;


    public static Intent newIntent(Context packageContext, ArrayList<UserMarker> userMarkers) {
        Intent intent = new Intent(packageContext, ArActivity.class);
        intent.putParcelableArrayListExtra(BUNDLE_MARKERS_LIST, userMarkers);
        return intent;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);
        mSurfaceView = findViewById(R.id.surfaceview);
        mDisplayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

        mMarkerList = new ArrayList<>();
        if (Objects.requireNonNull(getIntent().getExtras())
                .getParcelableArrayList(BUNDLE_MARKERS_LIST) != null) {
            mMarkerList = getIntent().getExtras().getParcelableArrayList(BUNDLE_MARKERS_LIST);
        }

        // Set up tap listener.
        mTapHelper = new TapHelper(/*context=*/ this);
        mSurfaceView.setOnTouchListener(mTapHelper);

        // Set up renderer.
        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        mSurfaceView.setRenderer(this);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        mInstallRequested = false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSession == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !mInstallRequested)) {
                    case INSTALL_REQUESTED:
                        mInstallRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                // Create the mSession.
                mSession = new Session(/* context= */ this);

            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR mSession";
                exception = e;
            }

            if (message != null) {
                mMessageSnackbarHelper.showError(this, message);
                Log.e(TAG, "Exception creating mSession", exception);
                return;
            }
        }

        mLocationScene = new LocationScene(this, this, mSession);
        updateMarkers();


        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            mSession.resume();
            mLocationScene.resume();
        } catch (CameraNotAvailableException e) {
            // In some cases (such as another camera app launching) the camera may be given to
            // a different app instead. Handle this properly by showing a message and recreate the
            // mSession at the next iteration.
            mMessageSnackbarHelper.showError(this, "Camera not available. Please restart the app.");
            mSession = null;
            return;
        }

        mSurfaceView.onResume();
        mDisplayRotationHelper.onResume();

        mMessageSnackbarHelper.showMessage(this, "Searching for surfaces...");
    }

    private void updateMarkers() {
        for (UserMarker marker : mMarkerList) {
            LocationMarker locationMarker = new LocationMarker(
                    marker.getUserLocation().getLongitude(),
                    marker.getUserLocation().getLatitude(),
                    new AnnotationRenderer(marker.getUser().getName())
            );
            locationMarker.setOnTouchListener(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ArActivity.this,
                            "Touched Aadam", Toast.LENGTH_SHORT).show();
                }
            });

            mLocationScene.mLocationMarkers.add(locationMarker);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSession != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the mSession. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call mSession.update() and get a SessionPausedException.
            mDisplayRotationHelper.onPause();
            mSurfaceView.onPause();
            mLocationScene.resume();
            mSession.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore mSession to be filled during update().
            mBackgroundRenderer.createOnGlThread(/*context=*/ this);
//            planeRenderer.createOnGlThread(/*context=*/ this, "models/trigrid.png");
//            pointCloudRenderer.createOnGlThread(/*context=*/ this);

            mVirtualObject.createOnGlThread(/*context=*/ this, "models/andy.obj", "models/andy.png");
            mVirtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

            mVirtualObjectShadow.createOnGlThread(
                    /*context=*/ this, "models/andy_shadow.obj", "models/andy_shadow.png");
            mVirtualObjectShadow.setBlendMode(ObjectRenderer.BlendMode.Shadow);
            mVirtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);

        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mDisplayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mSession == null) {
            return;
        }
        // Notify ARCore mSession that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        mDisplayRotationHelper.updateSessionIfNeeded(mSession);

        try {
            mSession.setCameraTextureName(mBackgroundRenderer.getTextureId());

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = mSession.update();
            Camera camera = frame.getCamera();

            // Handle taps. Handling only one tap per frame, as taps are usually low frequency
            // compared to frame rate.

//            MotionEvent tap = mTapHelper.poll();
//            if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
//                for (HitResult hit : frame.hitTest(tap)) {
//                    // Check if any plane was hit, and if it was hit inside the plane polygon
//                    Trackable trackable = hit.getTrackable();
//                    // Creates an anchor if a plane or an oriented point was hit.
//                    if ((trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose()))
//                            || (trackable instanceof Point
//                            && ((Point) trackable).getOrientationMode()
//                            == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)) {
//                        // Hits are sorted by depth. Consider only closest hit on a plane or oriented point.
//                        // Cap the number of objects created. This avoids overloading both the
//                        // rendering system and ARCore.
//                        if (mAnchors.size() >= 20) {
//                            mAnchors.get(0).detach();
//                            mAnchors.remove(0);
//                        }
//                        // Adding an Anchor tells ARCore that it should track this position in
//                        // space. This anchor is created on the Plane to place the 3D model
//                        // in the correct position relative both to the world and to the plane.
//                        mAnchors.add(hit.createAnchor());
//                        break;
//                    }
//                }
//            }

            // Draw background.
            mBackgroundRenderer.draw(frame);

            mLocationScene.draw(frame);

            // If not tracking, don't draw 3d objects.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            // The first three components are color scaling factors.
            // The last one is the average pixel intensity in gamma space.
            final float[] colorCorrectionRgba = new float[4];
            frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

            // Visualize tracked points.
//            PointCloud pointCloud = frame.acquirePointCloud();
//            pointCloudRenderer.update(pointCloud);
//            pointCloudRenderer.draw(viewmtx, projmtx);

            // Application is responsible for releasing the point cloud resources after
            // using it.
//            pointCloud.release();

            // Check if we detected at least one plane. If so, hide the loading message.
//            if (mMessageSnackbarHelper.isShowing()) {
//                for (Plane plane : mSession.getAllTrackables(Plane.class)) {
//                    if (plane.getType() == com.google.ar.core.Plane.Type.HORIZONTAL_UPWARD_FACING
//                            && plane.getTrackingState() == TrackingState.TRACKING) {
//            mMessageSnackbarHelper.hide(this);
//                        break;
//                    }
//                }
//            }

            // Visualize planes.
//            planeRenderer.drawPlanes(
//                    mSession.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);

            // Visualize mAnchors created by touch.
            float scaleFactor = 1.0f;
            for (Anchor anchor : mAnchors) {
                if (anchor.getTrackingState() != TrackingState.TRACKING) {
                    continue;
                }
                // Get the current pose of an Anchor in world space. The Anchor pose is updated
                // during calls to mSession.update() as ARCore refines its estimate of the world.
                anchor.getPose().toMatrix(mAnchorMatrix, 0);

                // Update and draw the model and its shadow.
                mVirtualObject.updateModelMatrix(mAnchorMatrix, scaleFactor);
                mVirtualObjectShadow.updateModelMatrix(mAnchorMatrix, scaleFactor);
                mVirtualObject.draw(viewmtx, projmtx, colorCorrectionRgba);
                mVirtualObjectShadow.draw(viewmtx, projmtx, colorCorrectionRgba);
            }

        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }
}
