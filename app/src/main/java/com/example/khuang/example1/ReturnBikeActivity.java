package com.example.khuang.example1;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

interface ReturnBikeConstants {
    public static final String clientId = MqttClient.generateClientId();
}

public class ReturnBikeActivity extends AppCompatActivity implements ReturnBikeConstants {
    MqttAndroidClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return_bike);

        // TODO: change this to a useful message
        String message = "You currently have 0 minutes remaining.";

        // Capture the layout's TextView and set the string as its text
        TextView textView = findViewById(R.id.bike_time_remaining);
        textView.setText(message);

        // create client
        client = new MqttAndroidClient(this.getApplicationContext(), Constants.HOSTNAME,
                ReturnBikeConstants.clientId); // create a new client when you open this page.

        // create a dropdown
        Spinner spinner = (Spinner) findViewById(R.id.spinner);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.return_locations, android.R.layout.simple_spinner_item);

        // create ArrayAdapter by querying all open locations. TODO: do this w/ DB.

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // adapterView.getItemAtPosition(i) should give you the thing user selected.
                Object obj  = adapterView.getItemAtPosition(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // do nothing?
            }
        });
    }

    IMqttActionListener publishAction = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            if (client.isConnected()) { publishReturn(client); }
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
            IMqttToken token = client.connect(); // completes async, so need to set a cb
            token.setActionCallback(publishAction);
        } catch (MqttException e) {
            e.printStackTrace();
            TextView returnErrorMsg = findViewById(R.id.return_server_error);
            returnErrorMsg.setVisibility(View.VISIBLE);
        }
    }

    private void publishReturn(MqttAndroidClient client) {
        // TODO: we should publish the return based on which location was selected.
        String topicName = "bikeshare/1";
        try {
            // qos = 2 ensures delivery (slow), retained --> broker will retain it
            IMqttDeliveryToken token = client.publish(topicName,
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

    private void queryControllersWrapper() {
        try {
            if (!client.isConnected()) {
                IMqttToken token = client.connect(); // completes async, so need to set a cb
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        if (client.isConnected()) { queryControllers(); }
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        TextView returnErrorMsg = findViewById(R.id.return_server_error);
                        returnErrorMsg.setVisibility(View.VISIBLE);
                    }
                });
            } else {
                queryControllers();
            }
        } catch (MqttException e) {
            e.printStackTrace();
            TextView returnErrorMsg = findViewById(R.id.return_server_error);
            returnErrorMsg.setVisibility(View.VISIBLE);
        }
    }

    private void queryControllers() {

    }
}
