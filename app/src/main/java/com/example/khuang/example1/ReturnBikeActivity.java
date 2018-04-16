package com.example.khuang.example1;

import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.mongodb.stitch.android.StitchClient;
import com.mongodb.stitch.android.auth.anonymous.AnonymousAuthProvider;

import org.bson.Document;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.ArrayList;
import java.util.List;

public class ReturnBikeActivity extends AppCompatActivity implements StitchClientListener {
    private MqttAndroidClient mqttClient;
    private StitchClient stitchClient;
    private final String clientId = MqttClient.generateClientId();
    private Object itemSelected = null;
    private static String userId = "";
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return_bike);

        Intent intent = getIntent();
        userId = intent.getStringExtra(Constants.EXTRA_MESSAGE);

        this.user = User.getUser(); // TODO: what if this is null, asshole?

        // create mqtt client
        this.mqttClient = new MqttAndroidClient(this.getApplicationContext(), Constants.HOSTNAME,
                clientId); // create a new client when you open this page.

        // initialize stitchclient
        StitchClientManager.initialize(this.getApplicationContext());
        StitchClientManager.registerListener(this);
    }

    @Override
    public void onReady(StitchClient stitchClient) {
        this.stitchClient = stitchClient;

        if (!this.stitchClient.isAuthenticated()) {
            this.stitchClient.logInWithProvider(new AnonymousAuthProvider()).addOnCompleteListener(new OnCompleteListener<String>() {
                @Override
                public void onComplete(@NonNull Task<String> task) {
                    if (task.isSuccessful()) { getLocations(); }
                    else { Exception e = task.getException(); }
                }
            });
        } else { getLocations(); }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // do this to preserve the userId from prev screen.
        }
        return true;
    }

    private void getLocations() {
        this.stitchClient.executeFunction("queryLocs", "0").addOnCompleteListener(new OnCompleteListener<Object>() {
            @Override
            public void onComplete(@NonNull Task<Object> task) {
                if (task.isSuccessful()) {
                    List<Document> result = (List<Document>) task.getResult(); // Document is a mongo class
                    List<String> locations = new ArrayList<String>();
                    for ( Document loc : result) {
                        Integer locationInt = (Integer) loc.get("location");
                        locations.add(Constants.locations.get(locationInt));
                    }
                    populateDropdown(locations);
                }
                else {
                    // Exception e = task.getException();
                    TextView returnErrorMsg = findViewById(R.id.return_server_error);
                    returnErrorMsg.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void populateDropdown(List<String> locs) {
        // create a dropdown
        Spinner spinner = (Spinner) findViewById(R.id.spinner);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, locs);

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectItem(adapterView.getItemAtPosition(i));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // TODO: do you maybe want to do something?
            }
        });
    }

    private void selectItem(Object obj) { this.itemSelected = obj; }

    public void returnBike(View view) {
        try {
            IMqttToken token = this.mqttClient.connect(); // completes async, so need to set a cb
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    publishReturn();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    TextView returnErrorMsg = findViewById(R.id.return_server_error);
                    returnErrorMsg.setVisibility(View.VISIBLE);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
            TextView returnErrorMsg = findViewById(R.id.return_server_error);
            returnErrorMsg.setVisibility(View.VISIBLE);
        }
    }

    private void publishReturn() {
        if (!this.mqttClient.isConnected()) { // if it's not connected, error.
            TextView returnErrorMsg = findViewById(R.id.return_server_error);
            returnErrorMsg.setVisibility(View.VISIBLE);
            return;
        }

        String locationName = (String) this.itemSelected;
        Integer locInt = Constants.invLocations.get(locationName);
        String topicName = "bikeshare/" + locInt.toString();

        try {
            String[] bikeId = this.user.getBikeId().split(" ");
            byte[] byteArray = stringArrayToByteArray(bikeId);
            IMqttDeliveryToken token = this.mqttClient.publish(topicName,
                    byteArray, Constants.QOS, true);
            token.setActionCallback(new IMqttActionListener() { // tokens are async, so set cb
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    TextView returnSuccessMsg = findViewById(R.id.return_success_msg);
                    returnSuccessMsg.setVisibility(View.VISIBLE);
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() { // close this activity after 5 seconds, after publish success
                        @Override
                        public void run() { finish(); }
                    }, 5000);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    TextView returnErrorMsg = findViewById(R.id.return_server_error);
                    returnErrorMsg.setVisibility(View.VISIBLE);
                }
            });
        } catch (MqttException | NullPointerException e) {
            e.printStackTrace();
            TextView returnErrorMsg = findViewById(R.id.return_server_error);
            returnErrorMsg.setVisibility(View.VISIBLE);
        }
    }

    // Returns the actual value of the byte. for example, "f" --> 15,
    // "fe" --> "1111 1110" in binary. sweet casting hell
    // TODO: test that it actually sends the correct byte array.
    private byte[] stringArrayToByteArray(String[] s_array) {
        byte byteArray[] = new byte[s_array.length];
        for (int i = 0; i < s_array.length; i++) {
            Integer returnVal = Integer.parseInt(s_array[i], 16);
            int returnIntVal = (int) returnVal;
            byteArray[i] = (byte) returnIntVal;
        }
        return byteArray;
    }
}