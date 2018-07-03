package com.aadimator.khoji.models;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.aadimator.khoji.R;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.concurrent.CompletableFuture;

import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;

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
    private float mDistance;
    private boolean mInRange;

    public UserMarker(User user, UserLocation userLocation) {
        mUser = user;
        mUserLocation = userLocation;
    }

    protected UserMarker(Parcel in) {
        this.mUser = in.readParcelable(User.class.getClassLoader());
        this.mUserLocation = in.readParcelable(UserLocation.class.getClassLoader());
        this.mDistance = in.readFloat();
        this.mInRange = in.readByte() != 0;
    }

    public void render(Context c, LocationScene locationScene) {

        CompletableFuture<ViewRenderable> couponLayout =
                ViewRenderable.builder()
                        .setView(c, R.layout.example_layout)
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
                                TextView title = vr.getView().findViewById(R.id.nodeName);

                                title.setText(mUser.getName());


                                LocationMarker couponLocationMarker = new LocationMarker(
                                        mUserLocation.getLongitude(),
                                        mUserLocation.getLatitude(),
                                        base
                                );

                                couponLocationMarker.setRenderEvent(node -> {
                                    View eView = vr.getView();
                                    TextView distanceTextView = eView.findViewById(R.id.nodeDistance);
                                    distanceTextView.setText(node.getDistance() + "M");
                                });

                                locationScene.mLocationMarkers.add(couponLocationMarker);

                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            return null;
                        });
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mUser, flags);
        dest.writeParcelable(this.mUserLocation, flags);
        dest.writeFloat(this.mDistance);
        dest.writeByte(this.mInRange ? (byte) 1 : (byte) 0);
    }
}
