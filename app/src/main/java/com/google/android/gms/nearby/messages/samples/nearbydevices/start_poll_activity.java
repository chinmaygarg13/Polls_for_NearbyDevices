package com.google.android.gms.nearby.messages.samples.nearbydevices;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.Messages;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;

import java.util.ArrayList;
import java.util.List;

import java.util.UUID;

public class start_poll_activity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private FloatingActionButton mButton;
    private FloatingActionButton mStopButton;
    private EditText mEdit;
    private TextView mText;

    private static final int TTL_IN_SECONDS = 1 * 60; // Three minutes.

    private static final Strategy PUB_SUB_STRATEGY = new Strategy.Builder()
            .setTtlSeconds(TTL_IN_SECONDS).build();

    // Key used in writing to and reading from SharedPreferences.
    private static final String KEY_UUID = "key_uuid";
    /**
     * Creates a UUID and saves it to {@link SharedPreferences}. The UUID is added to the published
     * message to avoid it being undelivered due to de-duplication. See {@link DeviceMessage} for
     * details.
     */
    private static String getUUID(SharedPreferences sharedPreferences) {
        String uuid = sharedPreferences.getString(KEY_UUID, "");
        if (TextUtils.isEmpty(uuid)) {
            uuid = UUID.randomUUID().toString();
            sharedPreferences.edit().putString(KEY_UUID, uuid).apply();
        }
        return uuid;
    }

    /**
     * The {@link Message} object used to broadcast information about the device to nearby devices.
     */
    private Message mPubMessage;

    private MessageListener mMessageListener;

    public GoogleApiClient mGoogleApiClient;

    private ArrayAdapter<String> mAnswersArrayAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_poll);

        mButton = (FloatingActionButton) findViewById(R.id.share_poll_button);
        mEdit   = (EditText)findViewById(R.id.edit_qa);
        mText = (TextView) findViewById(R.id.textView);
        mStopButton = (FloatingActionButton) findViewById(R.id.stop_sub_button);

        mButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        String message = mEdit.getText().toString();
                        mPubMessage = DeviceMessage.newNearbyMessage(getUUID(getSharedPreferences(
                                getApplicationContext().getPackageName(), Context.MODE_PRIVATE)), message);
                        Log.v("EditText", mEdit.getText().toString());

                        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()){
                            publish();
                            view.setVisibility(View.GONE);
                            mText.setVisibility(View.GONE);
                        }

                        //ViewGroup parentView = (ViewGroup) view.getParent();
                        //parentView.removeView(view);
                    }
                });

        mStopButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        unsubscribe();
                        view.setVisibility(View.INVISIBLE);
                        logAndShowSnackbar("Subscription off. Please see the result.");
                    }
                });

        mMessageListener = new MessageListener() {
            @Override
            public void onFound(final Message message) {
                // Called when a new message is found.
                mAnswersArrayAdapter.add(
                        DeviceMessage.fromNearbyMessage(message).getMessageBody());
            }
        };

        final List<String> answersArrayList = new ArrayList<>();
        mAnswersArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                answersArrayList);
        final ListView answersListView = (ListView) findViewById(
                R.id.ans_list);
        if (answersListView != null) {
            answersListView.setAdapter(mAnswersArrayAdapter);
        }

        buildGoogleApiClient();
    }

    public void buildGoogleApiClient() {
        if (mGoogleApiClient != null) {
            return;
        }
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .build();
    }

    private void publish() {
        Log.i("_log", "Publishing");
        PublishOptions options = new PublishOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new PublishCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.i("_log", "No longer publishing");
                        subscribe();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
logAndShowSnackbar("No longer publishing");                            }
                        });
                    }
                }).build();

        Nearby.Messages.publish(mGoogleApiClient, mPubMessage, options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            logAndShowSnackbar("Published successfully. Automatic unpublishing after 3 min.");
                        } else {
                            logAndShowSnackbar("Could not publish, status = " + status);
                        }
                    }
                });
    }

    private void subscribe() {
        Log.i("_log", "Subscribing");
        mAnswersArrayAdapter.clear();
        mStopButton.show();
        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new SubscribeCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.i("_log", "No longer subscribing");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                logAndShowSnackbar("No longer subscribing");
                            }
                        });
                    }
                }).build();

        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            logAndShowSnackbar("No longer Publishing. Subscription started");
                        } else {
                            logAndShowSnackbar("Could not subscribe, status = " + status);
                        }
                    }
                });
    }

    private void unpublish() {
        Log.i("_log", "Unpublishing.");
        Nearby.Messages.unpublish(mGoogleApiClient, mPubMessage);
    }

    private void unsubscribe() {
        Log.i("_log", "Unsubscribing.");
        logAndShowSnackbar("unsubscribed");
        Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i("_log", "GoogleApiClient connected");

    }

    @Override
    public void onConnectionSuspended(int i) {
        logAndShowSnackbar("Connection suspended. Error code: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        logAndShowSnackbar("Exception while connecting to Google Play services: " +
                connectionResult.getErrorMessage());
    }

    private void logAndShowSnackbar(final String text) {
        Log.w("_log", text);
        View container = findViewById(R.id.start_poll_container);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }
}
