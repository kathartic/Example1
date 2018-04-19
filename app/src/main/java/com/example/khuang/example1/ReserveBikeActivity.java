package com.example.khuang.example1;

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
import java.util.HashMap;
import java.util.List;

public class ReserveBikeActivity extends AppCompatActivity implements StitchClientListener{
    private User user;
    private StitchClient stitchClient;
    private Object itemSelected = null;
    private MqttAndroidClient mqttClient;
    private final String clientId = MqttClient.generateClientId();
    private HashMap<Integer, String> locationsToBikeIds = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reserve_bike);

        this.user = User.getUser(); // TODO: man i sure hope this isn't null.

        // create mqtt client
        this.mqttClient = new MqttAndroidClient(this.getApplicationContext(), Constants.HOSTNAME,
                clientId); // create a new client when you open this page.

        StitchClientManager.initialize(this.getApplicationContext());
        StitchClientManager.registerListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // do this to preserve the userId from prev screen.
        }
        return true;
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

    private void getLocations() {
        this.stitchClient.executeFunction("queryLocs").addOnCompleteListener(new OnCompleteListener<Object>() {
            @Override
            public void onComplete(@NonNull Task<Object> task) {
                if (task.isSuccessful()) {
                    try{
                        List<Document> result = (List<Document>) task.getResult(); // Document is a mongo class
                        List<String> locations = getLocationList(result);
                        populateDropdown(locations);
                    } catch(ClassCastException e) {
                        TextView resultMsg = findViewById(R.id.reserve_feedback);
                        resultMsg.setText(R.string.reserve_failure);
                        resultMsg.setVisibility(View.VISIBLE);
                    }
                } else {
                    TextView resultMsg = findViewById(R.id.reserve_feedback);
                    resultMsg.setText(R.string.reserve_failure);
                    resultMsg.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private List<String> getLocationList(List<Document> result) {
        List<String> locations = new ArrayList<String>();
        for ( Document loc : result) {
            locations.add((String) loc.get("stringName"));
            this.locationsToBikeIds.put((Integer) loc.get("id"), (String) loc.get("bikeId"));
        }
        return locations;
    }

    private void populateDropdown(List<String> locs) {
        Spinner spinner = (Spinner) findViewById(R.id.spinner2);
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, locs);
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

    public void reserveBike(View view) {
        if (this.itemSelected == null) { // TODO: show an error statement here.
            return;
        }
        try {
            IMqttToken token = this.mqttClient.connect(); // completes async, so need to set a cb
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    publishReserve();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    TextView checkoutErrorMsg = findViewById(R.id.reserve_feedback);
                    checkoutErrorMsg.setText(R.string.reserve_failure_mqtt);
                    checkoutErrorMsg.setVisibility(View.VISIBLE);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
            TextView checkoutErrorMsg = findViewById(R.id.reserve_feedback);
            checkoutErrorMsg.setText(R.string.reserve_failure_mqtt);
            checkoutErrorMsg.setVisibility(View.VISIBLE);
        }
    }

    public void publishReserve() {
        if (!this.mqttClient.isConnected()) { // if it's not connected, error.
            TextView checkoutErrorMsg = findViewById(R.id.reserve_feedback);
            checkoutErrorMsg.setText(R.string.reserve_failure_mqtt);
            checkoutErrorMsg.setVisibility(View.VISIBLE);
            return;
        }

        String locationName = (String) this.itemSelected;
        final Integer locInt = Constants.invLocations.get(locationName); // TODO: make sure matching
        String topicName = "bikeshare/" + locInt.toString();

        try {
            IMqttDeliveryToken token = this.mqttClient.publish(topicName, Constants.UNLOCK, Constants.QOS, true);
            token.setActionCallback(new IMqttActionListener() { // tokens are async, so set cb
                @Override
                public void onSuccess(IMqttToken asyncActionToken) { updateDbsAndFinish(locInt); }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    TextView checkoutErrorMsg = findViewById(R.id.reserve_feedback);
                    checkoutErrorMsg.setText(R.string.reserve_failure_mqtt);
                    checkoutErrorMsg.setVisibility(View.VISIBLE);
                }
            });
        } catch (MqttException | NullPointerException e) {
            e.printStackTrace();
            TextView checkoutErrorMsg = findViewById(R.id.reserve_feedback);
            checkoutErrorMsg.setText(R.string.reserve_failure_mqtt);
            checkoutErrorMsg.setVisibility(View.VISIBLE);
        }
    }

    private void updateDbsAndFinish(Integer location) {
        this.stitchClient.executeFunction("reserveBike", location, this.user.getUid(), this.locationsToBikeIds.get(location)).addOnCompleteListener(new OnCompleteListener<Object>() {
            @Override
            public void onComplete(@NonNull Task<Object> task) {
                refreshUserAndFinish();
            }
        });
    }

    private void refreshUserAndFinish() {
        this.stitchClient.executeFunction("getUser", this.user.getUid()).addOnCompleteListener(new OnCompleteListener<Object>() {
            @Override
            public void onComplete(@NonNull Task<Object> task) {
                try {
                    Document doc = (Document) task.getResult();
                    User.makeUserFromDoc(doc);
                    setUser();
                    TextView resultMsg = findViewById(R.id.reserve_feedback);
                    if (task.isSuccessful()) {
                        resultMsg.setText(R.string.reserve_success);
                        resultMsg.setVisibility(View.VISIBLE);
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() { // close this activity after 5 seconds, after publish success
                            @Override
                            public void run() { finish(); }
                        }, 5000);
                    } else {
                        resultMsg.setText(R.string.reserve_failure);
                        resultMsg.setVisibility(View.VISIBLE);
                    }
                } catch(ClassCastException e ) { // if you can't cast it to ad ocument, this means its bad.
                    TextView resultMsg = findViewById(R.id.reserve_feedback);
                    resultMsg.setText(R.string.reserve_failure);
                    resultMsg.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void setUser() {
        this.user = User.getUser();
    }
}
