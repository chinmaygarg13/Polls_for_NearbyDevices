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
import android.widget.ListView;

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

public class first_page_activity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private FloatingActionButton start_button;
    private FloatingActionButton participate_button;

    public GoogleApiClient mGoogleApiClient;

    private MessageListener mMessageListener;

    private static final int TTL_IN_SECONDS = 3 * 60; // Three minutes.

    private static final Strategy PUB_SUB_STRATEGY = new Strategy.Builder()
            .setTtlSeconds(TTL_IN_SECONDS).build();

    public String question;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_page);

        start_button = (FloatingActionButton) findViewById(R.id.start_button);
        participate_button = (FloatingActionButton) findViewById(R.id.participate_button);

        participate_button.setOnClickListener(mParticipateListener);
        start_button.setOnClickListener(mStartListener);

        mMessageListener = new MessageListener() {
            @Override
            public void onFound(final Message message) {
                // Called when a new message is found.
                question = DeviceMessage.fromNearbyMessage(message).getMessageBody();
                Intent myIntent = new Intent(first_page_activity.this, get_poll_activity.class);
                myIntent.putExtra("Question", "" + question);
                first_page_activity.this.startActivity(myIntent);
                unsubscribe();
            }
        };

        buildGoogleApiClient();
    }

    private View.OnClickListener mStartListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent myIntent = new Intent(first_page_activity.this, start_poll_activity.class);
            first_page_activity.this.startActivity(myIntent);
        }
    };

    private View.OnClickListener mParticipateListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            subscribe();
            View container = findViewById(R.id.first_page_container);
            Snackbar.make(container,"Subscribing...   Please WAIT, listening for messages.", Snackbar.LENGTH_INDEFINITE).show();
//            Intent myIntent = new Intent(first_page_activity.this, get_poll_activity.class);
//            myIntent.putExtra("Question", question);
//            first_page_activity.this.startActivity(myIntent);
        }
    };

    private void subscribe() {
        Log.i("_log", "Subscribing");
        //mText.clearComposingText();
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
        View container = findViewById(R.id.first_page_container);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }
}
