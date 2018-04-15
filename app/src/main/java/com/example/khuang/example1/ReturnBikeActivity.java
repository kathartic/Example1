package com.example.khuang.example1;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return_bike);

        // TODO: change this to a useful message
        String message = "You currently have 0 minutes remaining.";

        // Capture the layout's TextView and set the string as its text
        TextView textView = findViewById(R.id.bike_time_remaining);
        textView.setText(message);

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
        } else {
            getLocations();
        }
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
                else { Exception e = task.getException(); }
            }
        });
    }

    private void populateDropdown(List<String> locs) {
        // create a dropdown
        Spinner spinner = (Spinner) findViewById(R.id.spinner);

        // Create an ArrayAdapter using the string array and a default spinner layout
        // ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
         //       R.array.return_locations, android.R.layout.simple_spinner_item);

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, locs);

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // adapterView.getItemAtPosition(i) should give you the thing user selected.
                itemSelected = adapterView.getItemAtPosition(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // do nothing?
            }
        });
    }

    // does: upon connection, subscribes to the confirmations topic, then immediately
    // goes to publish.
    private IMqttActionListener returnAction = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            if (mqttClient.isConnected()) { publishReturn(); }
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            TextView returnErrorMsg = findViewById(R.id.return_server_error);
            returnErrorMsg.setVisibility(View.VISIBLE);
        }
    };

    public void returnBike(View view) {
        // TODO: use mqtt protocol to return the bike.
        try {
            IMqttToken token = this.mqttClient.connect(); // completes async, so need to set a cb
            token.setActionCallback(returnAction);
        } catch (MqttException e) {
            e.printStackTrace();
            TextView returnErrorMsg = findViewById(R.id.return_server_error);
            returnErrorMsg.setVisibility(View.VISIBLE);
        }
    }

    private void publishReturn() {
        // TODO: we should publish the return based on which location was selected.
        String topicName = "bikeshare/1";
        try {
            // qos = 2 ensures delivery (slow), retained --> broker will retain it
            IMqttDeliveryToken token = this.mqttClient.publish(topicName,
                    Constants.LOCK, Constants.QOS, true);
            token.setActionCallback(new IMqttActionListener() { // tokens are async, so set cb
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    TextView returnErrorMsg = findViewById(R.id.return_success_msg);
                    returnErrorMsg.setVisibility(View.VISIBLE);
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
}
