package com.aadimator.khoji.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.aadimator.khoji.R;
import com.aadimator.khoji.common.helpers.GlideApp;
import com.aadimator.khoji.models.User;
import com.aadimator.khoji.common.Constant;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ContactsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ContactsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ContactsFragment extends Fragment {

    private final String TAG = ContactsFragment.class.getSimpleName();

    private OnFragmentInteractionListener mListener;
    private Context mContext;
    private Activity mActivity;
    private FirebaseUser mCurrentUser;

    private FirebaseRecyclerAdapter mRecyclerAdapter;

    @BindView(R.id.edit_text_email)
    EditText editTextEmail;

    @BindView(R.id.button_add_contact)
    Button buttonAddContact;

    @BindView(R.id.recycler_view_contacts)
    RecyclerView mRecyclerViewContacts;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ContactsFragment.
     */
    public static ContactsFragment newInstance() {
        return new ContactsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        ButterKnife.bind(this, view);

        buttonAddContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = editTextEmail.getText().toString();
                addContact(email);
            }
        });

        mRecyclerAdapter = createRecyclerAdapater();
        mRecyclerViewContacts.setLayoutManager(new LinearLayoutManager(mContext));
        mRecyclerViewContacts.setAdapter(mRecyclerAdapter);
        return view;
    }

    private FirebaseRecyclerAdapter createRecyclerAdapater() {
        Query keyQuery = FirebaseDatabase.getInstance()
                .getReference(Constant.FIREBASE_URL_CONTACTS)
                .child(mCurrentUser.getUid())
                .orderByValue();

        DatabaseReference dataRef = FirebaseDatabase.getInstance().getReference(Constant.FIREBASE_URL_USERS);
        FirebaseRecyclerOptions<User> options =
                new FirebaseRecyclerOptions.Builder<User>()
                        .setIndexedQuery(keyQuery, dataRef, User.class)
                        .build();

        return new FirebaseRecyclerAdapter<User, UserHolder>(options) {
            @Override
            public UserHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.contacts_list, parent, false);

                return new UserHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull UserHolder holder, final int position, @NonNull final User model) {
                holder.mTextViewName.setText(model.getName());
                GlideApp.with(mActivity)
                        .load(model.getPhotoUrl())
                        .placeholder(R.drawable.com_facebook_profile_picture_blank_square)
                        .circleCrop()
                        .into(holder.mImageViewProfile);
                holder.mButtonDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String key = mRecyclerAdapter.getRef(position).getKey();
                        FirebaseDatabase.getInstance()
                                .getReference(Constant.FIREBASE_URL_CONTACTS)
                                .child(mCurrentUser.getUid())
                                .child(key)
                                .removeValue();
                    }
                });

                holder.mContactView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mListener.onContactSelection(mRecyclerAdapter.getRef(position).getKey());
                    }
                });
            }

            @Override
            public void onError(@NonNull DatabaseError error) {
                super.onError(error);
                Log.i(TAG, "Error getting data");
            }
        };
    }

    private void addContact(final String email) {
        if (email.equals(mCurrentUser.getEmail())) {
            return;
        }
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference users = database.getReference(Constant.FIREBASE_URL_USERS);
        Query query = users.orderByChild("email").equalTo(email);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        User user = userSnapshot.getValue(User.class);
                        database.getReference()
                                .child(Constant.FIREBASE_URL_CONTACTS)
                                .child(mCurrentUser.getUid())
                                .child(userSnapshot.getKey())
                                .setValue(user.getName());
                        Toast.makeText(mContext, getString(R.string.contact_added, email), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.i(TAG, "No user with " + email + " email");
                    Toast.makeText(mContext, getString(R.string.email_not_found, email), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mRecyclerAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        mRecyclerAdapter.stopListening();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
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
    public void onDetach() {
        super.onDetach();
        mListener = null;
        mContext = null;
        mActivity = null;
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onContactSelection(String uid);
    }

    class UserHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.imageViewProfile)
        ImageView mImageViewProfile;
        @BindView(R.id.textViewName)
        TextView mTextViewName;
        @BindView(R.id.buttonDelete)
        ImageButton mButtonDelete;
        @BindView(R.id.contactView)
        ConstraintLayout mContactView;

        UserHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
