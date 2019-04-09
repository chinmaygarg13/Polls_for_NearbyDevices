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

import java.util.Objects;
import java.util.UUID;
public class get_poll_activity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private FloatingActionButton mButton;
    private EditText mEditName;
    private EditText mEditAns;
    private TextView mText;

    private static final int TTL_IN_SECONDS = 3 * 60; // Three minutes.

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


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.get_poll);

//        mMessageListener = new MessageListener() {
//            @Override
//            public void onFound(final Message message) {
//                // Called when a new message is found.
//                String question = DeviceMessage.fromNearbyMessage(message).getMessageBody();
//                mText.setText(question);
//                unsubscribe();
//            }
//        };

        Intent intent = getIntent();

        String question = intent.getStringExtra("Question");
        mButton = (FloatingActionButton) findViewById(R.id.submit_button);
        mEditName = (EditText)findViewById(R.id.edit_name);
        mEditAns = (EditText) findViewById(R.id.edit_ans);
        mText = (TextView) findViewById(R.id.text_q);
        mText.setText(question);


//        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
//            subscribe();
//        }
//        else
//            logAndShowSnackbar("yeh kya ho raha hai???");

        mButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        String message = mEditName.getText().toString() + ":   " + mEditAns.getText().toString();
                        mPubMessage = DeviceMessage.newNearbyMessage(getUUID(getSharedPreferences(
                                getApplicationContext().getPackageName(), Context.MODE_PRIVATE)), message);
                        Log.v("EditText", message);

                        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()){
                            publish();
                        }
                    }
                });

        buildGoogleApiClient();

    }

//    @Override
//    public void onStart() {
//
//        super.onStart();
//
//        mMessageListener = new MessageListener() {
//            @Override
//            public void onFound(final Message message) {
//                // Called when a new message is found.
//                String question = DeviceMessage.fromNearbyMessage(message).getMessageBody();
//                mText.setText(question);
//                unsubscribe();
//            }
//        };
//
//        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
//            subscribe();
//        }
//        else
//            logAndShowSnackbar("yeh kya ho raha hai???");
//    }
//    @Override
//    public void onPause() {
//        super.onPause();
//
//        mMessageListener = new MessageListener() {
//            @Override
//            public void onFound(final Message message) {
//                // Called when a new message is found.
//                String question = DeviceMessage.fromNearbyMessage(message).getMessageBody();
//                mText.setText(question);
//                unsubscribe();
//            }
//        };
//
//        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
//            subscribe();
//        } else
//            logAndShowSnackbar("yeh kya ho raha hai???");
//
//    }

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
                            logAndShowSnackbar("Published successfully.");
                        } else {
                            logAndShowSnackbar("Could not publish, status = " + status);
                        }
                    }
                });
    }

    private void unpublish() {
        Log.i("_log", "Unpublishing.");
        Nearby.Messages.unpublish(mGoogleApiClient, mPubMessage);
    }

    private void subscribe() {
        Log.i("_log", "Subscribing");
        mText.clearComposingText();
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
                            Log.i("_log", "Subscribed successfully.");
                            logAndShowSnackbar("Subscription started");
                        } else {
                            logAndShowSnackbar("Could not subscribe, status = " + status);
                        }
                    }
                });
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
        View container = findViewById(R.id.get_poll_container);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }
}
