package com.aadimator.khoji.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.aadimator.khoji.R;
import com.aadimator.khoji.activities.BaseActivity;
import com.aadimator.khoji.utils.GlideApp;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * A simple {@link Fragment} subclass.
 */
public class AccountFragment extends Fragment {

    private Context mContext;
    private Activity mActivity;
    private FirebaseUser mUser;

    @BindView(R.id.logout)
    Button logoutButton;

    @BindView(R.id.profileImage)
    ImageView profileImage;

    @BindView(R.id.userName)
    TextView userName;

    public static AccountFragment newInstance() {
        return new AccountFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_account, container, false);
        ButterKnife.bind(this, view);

        String name = (mUser.getDisplayName() != null) ?
                mUser.getDisplayName() : mUser.getEmail();
        userName.setText(name);

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AuthUI.getInstance()
                        .signOut(mContext)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull com.google.android.gms.tasks.Task<Void> task) {
                                startActivity(new Intent(mContext, BaseActivity.class));
                            }
                        });
                mActivity.finish();
            }
        });

        GlideApp.with(this)
                .load(mUser.getPhotoUrl())
                .placeholder(R.drawable.com_facebook_profile_picture_blank_square)
                .apply(RequestOptions.circleCropTransform())
                .into(profileImage);


        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        if (context instanceof Activity) {
            mActivity = (Activity) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
        mActivity = null;
    }
}
