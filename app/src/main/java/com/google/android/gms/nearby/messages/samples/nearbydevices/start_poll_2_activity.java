package com.google.android.gms.nearby.messages.samples.nearbydevices;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static java.lang.Character.isDigit;

public class start_poll_2_activity extends AppCompatActivity {
    /** Our handler to Nearby Connections. */
    private ConnectionsClient mConnectionsClient;

    /** The devices we've discovered near us. */
    private final Map<String, Endpoint> mDiscoveredEndpoints = new HashMap<>();

    /**
     * The devices we have pending connections to. They will stay pending until we call {@link
     * #acceptConnection(Endpoint)} or {@link #rejectConnection(Endpoint)}.
     */
    private final Map<String, Endpoint> mPendingConnections = new HashMap<>();

    /**
     * The devices we are currently connected to. For advertisers, this may be large. For discoverers,
     * there will only be one entry in this map.
     */
    private final Map<String, Endpoint> mEstablishedConnections = new HashMap<>();

    /**
     * True if we are asking a discovered device to connect to us. While we ask, we cannot ask another
     * device.
     */
    private boolean mIsConnecting = false;

    /** True if we are discovering. */
    private boolean mIsDiscovering = false;

    /** True if we are advertising. */
    private boolean mIsAdvertising = false;

    /** Callbacks for connections to other devices. */
    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    logD(
                            String.format(
                                    "onConnectionInitiated(endpointId=%s, endpointName=%s)",
                                    endpointId, connectionInfo.getEndpointName()));

                    Endpoint endpoint = new Endpoint(endpointId, connectionInfo.getEndpointName());
                    mPendingConnections.put(endpointId, endpoint);
                    start_poll_2_activity.this.onConnectionInitiated(endpoint, connectionInfo);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    logD(String.format("onConnectionResponse(endpointId=%s, result=%s)", endpointId, result));

                    // We're no longer connecting
                    mIsConnecting = false;

                    if (!result.getStatus().isSuccess()) {
                        logW(
                                String.format(
                                        "Connection failed. Received status %s.",
                                        start_poll_2_activity.toString(result.getStatus())));
                        onConnectionFailed(mPendingConnections.remove(endpointId));
                        return;
                    }
                    connectedToEndpoint(mPendingConnections.remove(endpointId));
                }

                @Override
                public void onDisconnected(String endpointId) {
                    if (!mEstablishedConnections.containsKey(endpointId)) {
                        logW("Unexpected disconnection from endpoint " + endpointId);
                        return;
                    }
                    disconnectedFromEndpoint(mEstablishedConnections.get(endpointId));
                }
            };

    /** Callbacks for payloads (bytes of data) sent from another device to us. */
    private final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    logD(String.format("onPayloadReceived(endpointId=%s, payload=%s)", endpointId, payload));
                    onReceive(mEstablishedConnections.get(endpointId), payload);
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    logD(
                            String.format(
                                    "onPayloadTransferUpdate(endpointId=%s, update=%s)", endpointId, update));
                }
            };

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

    private FloatingActionButton mButton;
    private FloatingActionButton mStopButton;
    private FloatingActionButton mStartSubButton;
    private EditText mEdit;
    private ListView answersListView;

    private ArrayAdapter mAnswersArrayAdapter;

    public int num = 2;     /**Default number of options*/
    private int height = 30;
    private String mPubMessage;

    /** Called when our Activity is first created. */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_poll);

        //To hide the soft keyboard upon getting into this activity, as it has an EditText
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mButton = findViewById(R.id.share_poll_button);
        mEdit   = findViewById(R.id.edit_qa);
        mStopButton = findViewById(R.id.stop_sub_button);
        mStartSubButton = findViewById(R.id.stop_pub_start_sub_button);
        answersListView = findViewById(R.id.ans_list);
        final EditText mOpt1 = findViewById(R.id.edit_option1);
        final EditText mOpt2 = findViewById(R.id.edit_option2);
        Button mAddButton = findViewById(R.id.add_button);

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
                            mPubMessage = question + "$$" + opt1 + "$$" + opt2;
                            Log.v("_log",mPubMessage);

                            for(int i = 3; i <= num; i++){
                                EditText ed = findViewById(i);
                                mPubMessage += "$$" + ed.getText().toString();
                                Log.v("_log",mPubMessage);
                            }

                            Log.v("_log", mPubMessage);

                            startAdvertising();
                        }
                    }
                });

        mStopButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        stopDiscovering();
                        view.setVisibility(View.INVISIBLE);
                        logAndShowSnackbar("Subscription off. Please see the result.");
                    }
                });

        mStartSubButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        stopAdvertising();
                        disconnectFromAllEndpoints();
                        startDiscovering();
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

        //Adapting ListView for the incoming answers
        final List<String> answersArrayList = new ArrayList<>();
        mAnswersArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                answersArrayList);
        //TODO: Arrange according to the options and show final analysis
        mAnswersArrayAdapter.setNotifyOnChange(true);
        answersListView.setAdapter(mAnswersArrayAdapter);


        mConnectionsClient = Nearby.getConnectionsClient(this);
    }


    public void addOption(){
        LinearLayout mLayout = findViewById(R.id.options_container);
        EditText editTextView = new EditText(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
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

    /**
     * Sets the device to advertising mode. It will broadcast to other devices in discovery mode.
     * Either {@link #onAdvertisingStarted()} or {@link #onAdvertisingFailed()} will be called once
     * we've found out if we successfully entered this mode.
     */
    protected void startAdvertising() {
        mConnectionsClient.stopAdvertising();
        mConnectionsClient.stopDiscovery();
        mIsAdvertising = true;
        final String localEndpointName = getName();

        AdvertisingOptions.Builder advertisingOptions = new AdvertisingOptions.Builder();
        advertisingOptions.setStrategy(getStrategy());

        mConnectionsClient
                .startAdvertising(
                        localEndpointName,
                        getServiceId(),
                        mConnectionLifecycleCallback,
                        advertisingOptions.build())
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                logV("Now advertising endpoint " + localEndpointName);
                                onAdvertisingStarted();
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIsAdvertising = false;
                                logW("startAdvertising() failed.", e);
                                onAdvertisingFailed();
                            }
                        });
    }

    /** Stops advertising. */
    protected void stopAdvertising() {
        if(isAdvertising())
            mConnectionsClient.stopAdvertising();
        mIsAdvertising = false;
        logAndShowSnackbar("No longer publishing");
    }

    /** Returns {@code true} if currently advertising. */
    protected boolean isAdvertising() {
        return mIsAdvertising;
    }

    /** Called when advertising successfully starts. Override this method to act on the event. */
    protected void onAdvertisingStarted() {
        mButton.hide();
        mStartSubButton.show();
        logAndShowSnackbar("Published successfully. Press Button for Unpublishing.");
    }

    /** Called when advertising fails to start. Override this method to act on the event. */
    protected void onAdvertisingFailed() {
        logAndShowSnackbar("Could not start publishing.");
    }

    /**
     * Called when a pending connection with a remote endpoint is created. Use {@link ConnectionInfo}
     * for metadata about the connection (like incoming vs outgoing, or the authentication token). If
     * we want to continue with the connection, call {@link #acceptConnection(Endpoint)}. Otherwise,
     * call {@link #rejectConnection(Endpoint)}.
     */
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
        acceptConnection(endpoint);
        //if(isAdvertising())
            //send();
    }

    /** Accepts a connection request. */
    protected void acceptConnection(final Endpoint endpoint) {
        mConnectionsClient
                .acceptConnection(endpoint.getId(), mPayloadCallback)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                logW("acceptConnection() failed.", e);
                            }
                        });
    }

    /** Rejects a connection request. */
    protected void rejectConnection(Endpoint endpoint) {
        mConnectionsClient
                .rejectConnection(endpoint.getId())
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                logW("rejectConnection() failed.", e);
                            }
                        });
    }

    /**
     * Sets the device to discovery mode. It will now listen for devices in advertising mode. Either
     * {@link #onDiscoveryStarted()} or {@link #onDiscoveryFailed()} will be called once we've found
     * out if we successfully entered this mode.
     */
    protected void startDiscovering() {
        mConnectionsClient.stopDiscovery();
        mIsDiscovering = true;
        mDiscoveredEndpoints.clear();
        mAnswersArrayAdapter.clear();
        DiscoveryOptions.Builder discoveryOptions = new DiscoveryOptions.Builder();
        discoveryOptions.setStrategy(Strategy.P2P_CLUSTER);
        mConnectionsClient
                .startDiscovery(
                        getServiceId(),
                        new EndpointDiscoveryCallback() {
                            @Override
                            public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                                logD(
                                        String.format(
                                                "onEndpointFound(endpointId=%s, serviceId=%s, endpointName=%s)",
                                                endpointId, info.getServiceId(), info.getEndpointName()));

                                if (getServiceId().equals(info.getServiceId())) {
                                    Endpoint endpoint = new Endpoint(endpointId, info.getEndpointName());
                                    mDiscoveredEndpoints.put(endpointId, endpoint);
                                    onEndpointDiscovered(endpoint);
                                }
                            }

                            @Override
                            public void onEndpointLost(String endpointId) {
                                logD(String.format("onEndpointLost(endpointId=%s)", endpointId));
                            }
                        },
                        discoveryOptions.build())
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                onDiscoveryStarted();
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIsDiscovering = false;
                                logW("startDiscovering() failed.", e);
                                onDiscoveryFailed();
                            }
                        });
    }

    /** Stops discovery. */
    protected void stopDiscovering() {
        if(isDiscovering())
            mConnectionsClient.stopDiscovery();
        mIsDiscovering = false;
        logAndShowSnackbar("No longer subscribing");
    }

    /** Returns {@code true} if currently discovering. */
    protected boolean isDiscovering() {
        return mIsDiscovering;
    }

    /** Called when discovery successfully starts. Override this method to act on the event. */
    protected void onDiscoveryStarted() {
        mStartSubButton.hide();
        mStopButton.show();
        logAndShowSnackbar("No longer Publishing. Subscription started");
    }

    /** Called when discovery fails to start. Override this method to act on the event. */
    protected void onDiscoveryFailed() {
        logAndShowSnackbar("Could not subscribe.");
    }

    /**
     * Called when a remote endpoint is discovered. To connect to the device, call {@link
     * #connectToEndpoint(Endpoint)}.
     */
    protected void onEndpointDiscovered(Endpoint endpoint) {
        connectToEndpoint(endpoint);
    }

    /** Disconnects from the given endpoint. */
    protected void disconnect(Endpoint endpoint) {
        mConnectionsClient.disconnectFromEndpoint(endpoint.getId());
        mEstablishedConnections.remove(endpoint.getId());
    }

    /** Disconnects from all currently connected endpoints. */
    protected void disconnectFromAllEndpoints() {
        for (Endpoint endpoint : mEstablishedConnections.values()) {
            mConnectionsClient.disconnectFromEndpoint(endpoint.getId());
        }
        mEstablishedConnections.clear();
    }

    /** Resets and clears all state in Nearby Connections. */
    protected void stopAllEndpoints() {
        mConnectionsClient.stopAllEndpoints();
        mIsAdvertising = false;
        mIsDiscovering = false;
        mIsConnecting = false;
        mDiscoveredEndpoints.clear();
        mPendingConnections.clear();
        mEstablishedConnections.clear();
    }

    /**
     * Sends a connection request to the endpoint. Either {@link #onConnectionInitiated(Endpoint,
     * ConnectionInfo)} or {@link #onConnectionFailed(Endpoint)} will be called once we've found out
     * if we successfully reached the device.
     */
    protected void connectToEndpoint(final Endpoint endpoint) {
        logV("Sending a connection request to endpoint " + endpoint);
        // Mark ourselves as connecting so we don't connect multiple times
        mIsConnecting = true;

        // Ask to connect
        mConnectionsClient
                .requestConnection(getName(), endpoint.getId(), mConnectionLifecycleCallback)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                logW("requestConnection() failed.", e);
                                mIsConnecting = false;
                                onConnectionFailed(endpoint);
                            }
                        });
    }

    /** Returns {@code true} if we're currently attempting to connect to another device. */
    protected final boolean isConnecting() {
        return mIsConnecting;
    }

    private void connectedToEndpoint(Endpoint endpoint) {
        logD(String.format("connectedToEndpoint(endpoint=%s)", endpoint));
        mEstablishedConnections.put(endpoint.getId(), endpoint);
        onEndpointConnected(endpoint);
    }

    private void disconnectedFromEndpoint(Endpoint endpoint) {
        logD(String.format("disconnectedFromEndpoint(endpoint=%s)", endpoint));
        mEstablishedConnections.remove(endpoint.getId());
        onEndpointDisconnected(endpoint);
    }


    /**
     * Called when a connection with this endpoint has failed. Override this method to act on the
     * event.
     */
    protected void onConnectionFailed(Endpoint endpoint) {
        logD("Connection has FAILED");
    }

    /** Called when someone has connected to us. Override this method to act on the event. */
    protected void onEndpointConnected(Endpoint endpoint) {
        if(isAdvertising()){
            //send();
            mConnectionsClient
                    .sendPayload(endpoint.getId(), Payload.fromBytes(mPubMessage.getBytes()))
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    logW("sendPayload() failed.", e);
                                }
                            });
        }
    }

    /** Called when someone has disconnected. Override this method to act on the event. */
    protected void onEndpointDisconnected(Endpoint endpoint) {}

    /** Returns a list of currently discovered endpoints. */
    protected Set<Endpoint> getDiscoveredEndpoints() {
        return new HashSet<>(mDiscoveredEndpoints.values());
    }

    /** Returns a list of currently connected endpoints. */
    protected Set<Endpoint> getConnectedEndpoints() {
        return new HashSet<>(mEstablishedConnections.values());
    }

    /**
     * Sends a {@link Payload} to all currently connected endpoints.
     *
     * param payload The data you want to send.
     */
//    protected void send() {
//        //can add a parameter String endpointId and return send(endpointId) ->
//        //mConnectionsClient.sendPayload(endpointId, payload);
//        send(mEstablishedConnections.keySet());
//    }
//
//    private void send(Set<String> endpoints) {
//        mConnectionsClient
//                .sendPayload(new ArrayList<>(endpoints), Payload.fromBytes(mPubMessage.getBytes()))
//                .addOnFailureListener(
//                        new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                logW("sendPayload() failed.", e);
//                            }
//                        });
//    }

    /**
     * Someone connected to us has sent us data. Override this method to act on the event.
     *
     * @param endpoint The sender.
     * @param payload The data.
     */
    protected void onReceive(Endpoint endpoint, Payload payload) {
        // Called when a new message is found.
        //TODO: Add Roll number or name of the sender
        mAnswersArrayAdapter.add(new String(payload.asBytes()));
        mAnswersArrayAdapter.sort(ALPHABETICAL_ORDER1);
        height += 130;
        ConstraintLayout.LayoutParams mParam = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT, height);
        answersListView.setLayoutParams(mParam);
    }

    /** Returns the client's name. Visible to others when connecting. */
    protected String getName(){
        return "Chinmay Garg";
        //TODO return roll number obtained from the app's shared preference
    }

    /**
     * Returns the service id. This represents the action this connection is for. When discovering,
     * we'll verify that the advertiser has the same service id before we consider connecting to them.
     */
    protected String getServiceId(){
        return "com.google.android.gms.nearby.messages.samples.nearbydevices";
    }

    /**
     * Returns the strategy we use to connect to other devices. Only devices using the same strategy
     * and service id will appear when discovering. Stragies determine how many incoming and outgoing
     * connections are possible at the same time, as well as how much bandwidth is available for use.
     */
    protected Strategy getStrategy(){
        return Strategy.P2P_STAR;
    }

    /**
     * Transforms a {@link Status} into a English-readable message for logging.
     *
     * @param status The current status
     * @return A readable String. eg. [404]File not found.
     */
    private static String toString(Status status) {
        return String.format(
                Locale.US,
                "[%d]%s",
                status.getStatusCode(),
                status.getStatusMessage() != null
                        ? status.getStatusMessage()
                        : ConnectionsStatusCodes.getStatusCodeString(status.getStatusCode()));
    }

    /**
     * Returns {@code true} if the app was granted all the permissions. Otherwise, returns {@code
     * false}.
     */
    public static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @CallSuper
    protected void logV(String msg) {
        Log.v("_log", msg);
    }

    @CallSuper
    protected void logD(String msg) {
        Log.d("_log", msg);
    }

    @CallSuper
    protected void logW(String msg) {
        Log.w("_log", msg);
    }

    @CallSuper
    protected void logW(String msg, Throwable e) {
        Log.w("_log", msg, e);
    }

    @CallSuper
    protected void logE(String msg, Throwable e) {
        Log.e("_log", msg, e);
    }

    private void logAndShowSnackbar(final String text) {
        Log.w("_log", text);
        View container = findViewById(R.id.start_poll_container);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }
}
