package com.example.khuang.example1;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.mongodb.stitch.android.StitchClient;
import com.mongodb.stitch.android.auth.anonymous.AnonymousAuthProvider;

import org.bson.BsonType;
import org.bson.BsonUndefined;
import org.bson.Document;

public class MainActivity extends AppCompatActivity implements StitchClientListener{
    private StitchClient stitchClient;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) { // loads on pull-up
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StitchClientManager.initialize(this.getApplicationContext());
        StitchClientManager.registerListener(this);
    }

    @Override
    public void onReady(StitchClient stitchClient) {
        this.stitchClient = stitchClient;
        if (!this.stitchClient.isAuthenticated()) {
            this.stitchClient.logInWithProvider(new AnonymousAuthProvider()).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {

                }
            });
        }
    }

    private void getCurrentUser(String userId) {
        this.stitchClient.executeFunction("getUser", userId).addOnCompleteListener(new OnCompleteListener<Object>() {
            public void onComplete(@NonNull Task<Object> task) {
                if (task.isSuccessful()) {
                    Object obj = task.getResult();
                    try {
                        Document doc = (Document) task.getResult();
                        User.makeUserFromDoc(doc);
                        setUser();
                    } catch(ClassCastException e ) { // if you can't cast it to ad ocument, this means its bad.
                        TextView errorIdLengthMsg = findViewById(R.id.error_id_length);
                        errorIdLengthMsg.setVisibility(View.VISIBLE);
                    }
                }
                else { Exception e = task.getException(); }
            }
        });
    }

    private void setUser() {
        this.user = User.getUser();
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        intent.putExtra(Constants.EXTRA_MESSAGE, this.user.getUid());
        TextView errorIdLengthMsg = findViewById(R.id.error_id_length);
        errorIdLengthMsg.setVisibility(View.INVISIBLE);
        startActivity(intent);
    }

    // triggered by log in
    public void sendMessage(View view) {
        EditText editText = (EditText) findViewById(R.id.editText);
        String message = editText.getText().toString();

        TextView errorIdLengthMsg = findViewById(R.id.error_id_length);
        if (message.length() == 9) { getCurrentUser(message); }
        else { errorIdLengthMsg.setVisibility(View.VISIBLE); }
    }
}
