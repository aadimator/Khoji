package com.aadimator.khoji.widgets;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.aadimator.khoji.R;
import com.aadimator.khoji.activities.MainActivity;
import com.aadimator.khoji.models.User;
import com.aadimator.khoji.common.Constant;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by aadim on 12/31/2017.
 */

public class ContactsWidgetViewFactory implements RemoteViewsService.RemoteViewsFactory {

    private static final String LOG_TAG = ContactsWidgetViewFactory.class.getName();

    // FireBase global variables
    private FirebaseDatabase mFirebaseDB;
    private DatabaseReference mDatabaseReference;
    private FirebaseUser mCurrentUser;

    private int appWidgetId;

    private Context mContext;
    private List<User> mContacts = new ArrayList<>();
    private List<String> mContactKeys = new ArrayList<>();
    private CountDownLatch mCountDownLatch;


    public ContactsWidgetViewFactory(Context context, Intent intent) {
        mContext = context;
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {
        mCountDownLatch = new CountDownLatch(1);
        populateListItem();
        try {
            mCountDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        return mContacts.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        final User user = mContacts.get(position);
        RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item);

        remoteViews.setTextViewText(R.id.textViewName, user.getName());

        // Next, we set a fill-intent which will be used to fill-in the pending intent template
        // which is set on the collection view in ContactsWidgetProvider.
        Intent fillInIntent = new Intent();
        fillInIntent.putExtra(MainActivity.BUNDLE_CONTACT_DATA, mContactKeys.get(position));
        remoteViews.setOnClickFillInIntent(R.id.contacts_list_item, fillInIntent);

        return remoteViews;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private void populateListItem() {
        mFirebaseDB = FirebaseDatabase.getInstance();
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mDatabaseReference = mFirebaseDB.getReference(Constant.FIREBASE_URL_CONTACTS)
                .child(mCurrentUser.getUid());
        mDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mContactKeys.clear();
                mContacts.clear();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    mFirebaseDB.getReference(Constant.FIREBASE_URL_USERS)
                            .child(child.getKey())
                            .orderByKey()
                            .addValueEventListener(
                                    new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            User user = dataSnapshot.getValue(User.class);
                                            mContacts.add(user);
                                            mContactKeys.add(dataSnapshot.getKey());
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    }
                            );
                }
                if (mCountDownLatch.getCount() == 0) {
                    Intent updateWidgetIntent = new Intent(mContext, ContactsWidgetProvider.class);
                    updateWidgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    mContext.sendBroadcast(updateWidgetIntent);
                } else {
                    mCountDownLatch.countDown();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}
