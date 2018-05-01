package com.example.khuang.example1;

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

public class CheckStatusActivity extends AppCompatActivity implements StitchClientListener {
    private StitchClient stitchClient;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_status);
        this.user = User.getUser(); // get old user object

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
                    if (task.isSuccessful()) { getUser(); }
                    else { Exception e = task.getException(); }
                }
            });
        } else { getUser(); }
    }

    private void getUser() {
        this.stitchClient.executeFunction("getUser", this.user.getUid()).addOnCompleteListener(new OnCompleteListener<Object>() {
            @Override
            public void onComplete(@NonNull Task<Object> task) {
                try {
                    Document doc = (Document) task.getResult();
                    User.makeUserFromDoc(doc);
                    setUser();
                } catch(ClassCastException e ) { // if you can't cast it to ad ocument, this means its bad.
                    TextView error_text = findViewById(R.id.check_status_error_text);
                    error_text.setText(R.string.reserve_failure);
                    error_text.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void setUser() {
        this.user = User.getUser();
        TextView bikeStatus = findViewById(R.id.user_bike_status);
        TextView pointsStatus = findViewById(R.id.points_status);

        // now set all the statuses.
        if (this.user.isBikeInUse()) { bikeStatus.setText(R.string.check_bike_status_true); }
        else { bikeStatus.setText(R.string.check_bike_status_false); }

        String points = "Points: " + this.user.getPoints().toString();
        pointsStatus.setText(points.subSequence(0, points.length()));
    }
}
