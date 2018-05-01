package com.example.khuang.example1;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.mongodb.stitch.android.StitchClient;
import com.mongodb.stitch.android.auth.anonymous.AnonymousAuthProvider;

import org.bson.Document;

public class DisplayMessageActivity extends AppCompatActivity implements StitchClientListener{
    private StitchClient stitchClient;
    private static String userId = "";
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_message);

        StitchClientManager.initialize(this.getApplicationContext());
        StitchClientManager.registerListener(this);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        userId = intent.getStringExtra(Constants.EXTRA_MESSAGE);
    }

    @Override
    public void onReady(StitchClient stitchClient) {
        this.stitchClient = stitchClient;
        if (!this.stitchClient.isAuthenticated()) {
            this.stitchClient.logInWithProvider(new AnonymousAuthProvider()).addOnCompleteListener(new OnCompleteListener<String>() {
                @Override
                public void onComplete(@NonNull Task<String> task) {
                    if (task.isSuccessful()) {
                        getCurrentUser();
                    } else { Exception e = task.getException(); }
                }
            });
        } else { getCurrentUser(); }
    }

    private void getCurrentUser() {
        if (User.getUser() == null) {
            this.stitchClient.executeFunction("getUser", this.userId).addOnCompleteListener(new OnCompleteListener<Object>() {
                public void onComplete(@NonNull Task<Object> task) {
                    if (task.isSuccessful()) {
                        try {
                            Document doc = (Document) task.getResult();
                            User.makeUserFromDoc(doc);
                            setUser();
                        } catch(ClassCastException e) {
                            e.printStackTrace();
                        }
                    }
                    else { Exception e = task.getException(); }
                }
            });
        } else { setUser(); }
    }

    private void setUser() {
        this.user = User.getUser();
        String message = "Welcome, " + this.user.getFirstName();

        // Capture the layout's TextView and set the string as its text
        TextView textView = findViewById(R.id.textView);
        textView.setText(message);
    }

    public void returnBike(View view) {
        if (this.user != null && this.user.isBikeInUse()) {
            Intent intent = new Intent(this, ReturnBikeActivity.class);
            startActivityForResult(intent, Constants.RETURN_BIKE_REQUEST);
        } else { // this will be triggered if the user is null OR the user doesn't have a bike checked out.
            TextView errorText = findViewById(R.id.error_text);
            errorText.setText(R.string.return_error);
            errorText.setVisibility(View.VISIBLE);
        }
    }

    public void reserveBike(View view) {
        if (this.user != null && !this.user.isBikeInUse()) {
            Intent intent = new Intent(this, ReserveBikeActivity.class);
            startActivityForResult(intent, Constants.RESERVE_BIKE_REQUEST);
        } else {
            TextView errorText = findViewById(R.id.error_text);
            errorText.setText(R.string.reserve_error);
            errorText.setVisibility(View.VISIBLE);
        }
    }

    public void checkStatus(View view) {
        if (this.user != null) {
            Intent intent = new Intent(this, CheckStatusActivity.class);
            startActivityForResult(intent, Constants.CHECK_STATUS_REQUEST);
        } else {
            TextView errorText = findViewById(R.id.error_text);
            errorText.setText(R.string.status_error);
            errorText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.RESERVE_BIKE_REQUEST || requestCode == Constants.CHECK_STATUS_REQUEST) {
            this.user = User.getUser();
        } else if (requestCode == Constants.RETURN_BIKE_REQUEST) {
            // TODO: stuffs
        }
        TextView errorText = findViewById(R.id.error_text);
        errorText.setVisibility(View.INVISIBLE);
    }
}
