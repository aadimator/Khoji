package com.aadimator.khoji.models;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.aadimator.khoji.R;
import com.aadimator.khoji.common.GlideApp;
import com.aadimator.khoji.common.Utilities;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.concurrent.CompletableFuture;

import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;
import uk.co.appoly.arcorelocation.rendering.LocationNode;
import uk.co.appoly.arcorelocation.rendering.LocationNodeRender;
import uk.co.appoly.arcorelocation.utils.LocationUtils;

public class UserMarker implements Parcelable {

    public static final Parcelable.Creator<UserMarker> CREATOR = new Parcelable.Creator<UserMarker>() {
        @Override
        public UserMarker createFromParcel(Parcel source) {
            return new UserMarker(source);
        }

        @Override
        public UserMarker[] newArray(int size) {
            return new UserMarker[size];
        }
    };
    private static final String TAG = UserMarker.class.getSimpleName();
    private User mUser;
    private UserLocation mUserLocation;
    private String mUserID;
    private float mDistance;
    private boolean mInRange;

    public UserMarker(User user, UserLocation userLocation) {
        mUser = user;
        mUserLocation = userLocation;
    }

    public UserMarker(String userID, User user, UserLocation userLocation) {
        mUserID = userID;
        mUser = user;
        mUserLocation = userLocation;
    }

    protected UserMarker(Parcel in) {
        this.mUser = in.readParcelable(User.class.getClassLoader());
        this.mUserLocation = in.readParcelable(UserLocation.class.getClassLoader());
        this.mUserID = in.readString();
        this.mDistance = in.readFloat();
        this.mInRange = in.readByte() != 0;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void render(Context c, LocationScene locationScene, OnMarkerTouchListener mListener) {

        CompletableFuture<ViewRenderable> couponLayout =
                ViewRenderable.builder()
                        .setView(c, R.layout.ar_marker_layout)
                        .build();


        CompletableFuture.allOf(couponLayout)
                .handle(
                        (notUsed, throwable) -> {

                            if (throwable != null) {
                                Log.i(TAG, "Unable to load renderables");
                                return null;
                            }

                            try {
                                // Non scalable info outside location
                                ViewRenderable vr = couponLayout.get();
                                Node base = new Node();
                                base.setRenderable(vr);

                                base.setOnTapListener((hitTestResult, motionEvent) -> {
                                    Toast.makeText(c, mUser.getName(), Toast.LENGTH_SHORT).show();
                                    mListener.onMarkerTouched(this);
                                });

                                TextView title = vr.getView().findViewById(R.id.nodeName);
                                title.setText(mUser.getName());

                                ImageView imageView = vr.getView().findViewById(R.id.nodeImage);

                                // To tint the placeholder
                                Drawable placeholder = Utilities.getTinted
                                        (
                                                c,
                                                R.drawable.user_avatar,
                                                c.getResources().getColor(R.color.flatHalfWhite)
                                        );

                                GlideApp.with(c)
                                        .load(mUser.getPhotoUrl())
                                        .placeholder(placeholder)
                                        .circleCrop()
                                        .into(imageView);

                                LocationMarker couponLocationMarker = new LocationMarker(
                                        mUserLocation.getLongitude(),
                                        mUserLocation.getLatitude(),
                                        base
                                );


                                couponLocationMarker.setRenderEvent(new LocationNodeRender() {
                                    @Override
                                    public void render(LocationNode locationNode) {
                                        View eView = vr.getView();
                                        TextView distanceTextView = eView.findViewById(R.id.nodeDistance);
                                        String distance = locationNode.getDistance() > 1000
                                                ? (locationNode.getDistance() * 0.001) + " km"
                                                : locationNode.getDistance() + " meters";
                                        distanceTextView.setText(distance);
                                    }
                                });

                                locationScene.mLocationMarkers.add(couponLocationMarker);
                                locationScene.refreshAnchors();

                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            return null;
                        });
    }


    public boolean locationChanged(UserLocation newLocation) {
        return mUserLocation.getLongitude() != newLocation.getLongitude() ||
                mUserLocation.getLatitude() != newLocation.getLatitude();
    }

    public boolean locationChangedBy(UserLocation newLocation, int limit) {
        int markerDistance = (int) Math.ceil(LocationUtils.distance(this.mUserLocation.getLatitude(), newLocation.getLatitude(), mUserLocation.getLongitude(), newLocation.getLongitude(), 0.0D, 0.0D));
        return markerDistance > limit;
    }

    public String getUserID() {
        return mUserID;
    }

    public void setUserID(String userID) {
        mUserID = userID;
    }

    public User getUser() {
        return mUser;
    }

    public void setUser(User user) {
        mUser = user;
    }

    public UserLocation getUserLocation() {
        return mUserLocation;
    }

    public void setUserLocation(UserLocation userLocation) {
        mUserLocation = userLocation;
    }

    public float getDistance() {
        return mDistance;
    }

    public void setDistance(float distance) {
        mDistance = distance;
    }

    public boolean isInRange() {
        return mInRange;
    }

    public void setInRange(boolean inRange) {
        mInRange = inRange;
    }

    public interface OnMarkerTouchListener {
        void onMarkerTouched(UserMarker marker);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mUser, flags);
        dest.writeParcelable(this.mUserLocation, flags);
        dest.writeString(this.mUserID);
        dest.writeFloat(this.mDistance);
        dest.writeByte(this.mInRange ? (byte) 1 : (byte) 0);
    }
}
