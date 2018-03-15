package com.example.khuang.example1;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.UnsupportedEncodingException;

interface ReturnBikeConstants {
    public static final String hostName = "tcp://130.58.167.93:1883";
    public static final String topicName = "outTopic";
    public static final String clientId = MqttClient.generateClientId();
    public static final int qos = 2;
}

public class ReturnBikeActivity extends AppCompatActivity implements ReturnBikeConstants {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return_bike);

        // TODO: change this to a useful message
        String message = "You currently have 0 minutes remaining.";

        // Capture the layout's TextView and set the string as its text
        TextView textView = findViewById(R.id.bike_time_remaining);
        textView.setText(message);

        // create a dropdown
        Spinner spinner = (Spinner) findViewById(R.id.spinner);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.return_locations, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
    }

    public void returnBike(View view) {
        // TODO: use mqtt protocol to return the bike.
        final MqttAndroidClient client =
                new MqttAndroidClient(this.getApplicationContext(), ReturnBikeConstants.hostName,
                        ReturnBikeConstants.clientId);
        try {
            IMqttToken token = client.connect(); // completes async, so need to set a cb
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) { // connection okay
                    if (client.isConnected()) { publishReturn(client); }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    TextView returnErrorMsg = findViewById(R.id.return_server_error);
                    returnErrorMsg.setVisibility(View.VISIBLE);
                }
            });

            // does this need to be async too????
            // TODO: how to make a successful disconnect?
            if(client.isConnected()) {
                client.disconnect();
                client.unregisterResources();
            }
        } catch (MqttException e) {
            e.printStackTrace();
            TextView returnErrorMsg = findViewById(R.id.return_server_error);
            returnErrorMsg.setVisibility(View.VISIBLE);
        }
    }

    private void publishReturn(MqttAndroidClient client) {
        String bike = "bike";
        try {
            byte[] payload = bike.getBytes("UTF-8");

            // qos = 2 ensures delivery (slow), retained --> broker will retain it
            IMqttDeliveryToken token = client.publish(ReturnBikeConstants.topicName,
                    payload, ReturnBikeConstants.qos, true);
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
        } catch (MqttException | UnsupportedEncodingException e) {
            e.printStackTrace();
            TextView returnErrorMsg = findViewById(R.id.return_server_error);
            returnErrorMsg.setVisibility(View.VISIBLE);
        }
    }
}
