package com.google.android.gms.nearby.messages.samples.nearbydevices;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import java.util.UUID;

import static java.lang.Character.isDigit;

public class start_poll_activity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private FloatingActionButton mButton;
    private FloatingActionButton mStopButton;
    private FloatingActionButton mStartSubButton;
    private EditText mEdit;

    private static final int TTL_IN_SECONDS = 10 * 60; // Ten minutes.

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

    private ArrayAdapter mAnswersArrayAdapter;

    public int num = 2;     /**Default number of options*/
    private int height = 30;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_poll);

        //To hide the soft keyboard upon getting into this activity, as it has an EditText
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //findViewbyId
        mButton = findViewById(R.id.share_poll_button);
        mEdit   = findViewById(R.id.edit_qa);
        mStopButton = findViewById(R.id.stop_sub_button);
        mStartSubButton = findViewById(R.id.stop_pub_start_sub_button);
        final ListView answersListView = findViewById(R.id.ans_list);
        final EditText mOpt1 = findViewById(R.id.edit_option1);
        final EditText mOpt2 = findViewById(R.id.edit_option2);
        Button mAddButton = findViewById(R.id.add_button);

        ViewCompat.setNestedScrollingEnabled(answersListView, true);
        //Listeners
        mButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        ConstraintLayout mainLayout = findViewById(R.id.start_poll_container);
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(mainLayout.getWindowToken(), 0);
                        }

                        String question = mEdit.getText().toString();
                        String opt1 = mOpt1.getText().toString();
                        String opt2 = mOpt2.getText().toString();

                        if(question.matches("")){
                            logAndShowSnackbar("No Question entered. Please enter your question first.");
                        }
                        else if(opt1.matches("1. ") || opt2.matches("2. ")){
                            logAndShowSnackbar("Enter at least the first 2 options.");
                        }
                        else{
                            String message = question + "$$" + opt1 + "$$" + opt2;
                            Log.v("_log",message);

                            for(int i = 3; i <= num; i++){
                                EditText ed = findViewById(i);
                                message += "$$" + ed.getText().toString();
                                Log.v("_log",message);
                            }

                            mPubMessage = DeviceMessage.newNearbyMessage(getUUID(getSharedPreferences(
                                getApplicationContext().getPackageName(), Context.MODE_PRIVATE)), message);
                            Log.v("_log", message);

                            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()){
                                publish();
                            }
                        }
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

        mStartSubButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        unpublish();
                        subscribe();
                        //view.setVisibility(View.GONE);
                        logAndShowSnackbar("Publishing Off. Now Receiving Answers.");
                    }
                });


        mAddButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        addOption();
                    }
                }
        );

        /**
         * It is very important that every option that is sent to the receiver must start
         * with its option number.
         * Otherwise app may crash.
         */

        final Comparator<String> ALPHABETICAL_ORDER1 = new Comparator<String>() {
            public int compare(String object1, String object2) {
                if(isDigit(object1.charAt(0)) && isDigit(object2.charAt(0))){
                    int i1 = Character.getNumericValue(object1.charAt(0));
                    int i2 = Character.getNumericValue(object2.charAt(0));
                    if(isDigit(object1.charAt(1))){
                        i1 = i1*10 + Character.getNumericValue(object1.charAt(1));
                    }
                    if(isDigit(object2.charAt(1))){
                        i2 = i2*10 + Character.getNumericValue(object2.charAt(1));
                    }
                    return Integer.compare(i1,i2);
                }
                else{
                    return String.CASE_INSENSITIVE_ORDER.compare(object1, object2);
                }
            }
        };

        mMessageListener = new MessageListener() {
            @Override
            public void onFound(final Message message) {
                // Called when a new message is found.
                //TODO: Add Roll number or name of the sender
                mAnswersArrayAdapter.add(
                        DeviceMessage.fromNearbyMessage(message).getMessageBody());
                mAnswersArrayAdapter.sort(ALPHABETICAL_ORDER1);
                height += 130;
                ConstraintLayout.LayoutParams mParam = new ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT, height);
                answersListView.setLayoutParams(mParam);
            }
        };

        //Adapting ListView for the incoming answers
        final List<String> answersArrayList = new ArrayList<>();
        mAnswersArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                answersArrayList);
        //TODO: Arrange according to the options and show final analysis
        mAnswersArrayAdapter.setNotifyOnChange(true);
        answersListView.setAdapter(mAnswersArrayAdapter);

        buildGoogleApiClient();
    }


    public void addOption(){
        LinearLayout mLayout = findViewById(R.id.options_container);
        EditText editTextView = new EditText(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        params.setMargins(dpToPx(20),0,dpToPx(10),0);
        editTextView.setLayoutParams(params);
        num++;
        editTextView.setId(num);
        editTextView.setText(num + ". ");
        mLayout.addView(editTextView);
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
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
                                logAndShowSnackbar("No longer publishing");
                            }
                        });
                    }
                }).build();

        Nearby.Messages.publish(mGoogleApiClient, mPubMessage, options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            mButton.hide();
                            mStartSubButton.show();
                            logAndShowSnackbar("Published successfully. " +
                                    "Automatic unpublishing after 10 min. Press Button for Instant Unpublishing.");
                        } else {
                            logAndShowSnackbar("Could not publish, status = " + status);
                        }
                    }
                });
    }

    private void subscribe() {
        Log.i("_log", "Subscribing");
        mAnswersArrayAdapter.clear();
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
                            mStartSubButton.hide();
                            mStopButton.show();
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
