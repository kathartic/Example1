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

public class DisplayMessageActivity extends AppCompatActivity implements StitchClientListener{
    private StitchClient stitchClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_message);

        StitchClientManager.initialize(this.getApplicationContext());
        StitchClientManager.registerListener(this);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String message = "Welcome, " + intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        // Capture the layout's TextView and set the string as its text
        TextView textView = findViewById(R.id.textView);
        textView.setText(message);
    }

    @Override
    public void onReady(StitchClient stitchClient) {
        this.stitchClient = stitchClient;
        if (!this.stitchClient.isAuthenticated()) {
            this.stitchClient.logInWithProvider(new AnonymousAuthProvider()).addOnCompleteListener(new OnCompleteListener<String>() {
                @Override
                public void onComplete(@NonNull Task<String> task) {
                    if (task.isSuccessful()) {
                        // TODO: add some kind of success message?
                    } else { Exception e = task.getException(); }
                }
            });
        }
    }

    private void getCurrentUser() {

    }

    public void returnBike(View view) {
        Intent intent = new Intent(this, ReturnBikeActivity.class);
        // intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    public void reserveBike(View view) {
        Intent intent = new Intent(this, ReserveBikeActivity.class);
        // intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }
}
