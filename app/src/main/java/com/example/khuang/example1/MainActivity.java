package com.example.khuang.example1;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    @Override
    protected void onCreate(Bundle savedInstanceState) { // loads on pull-up
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    // triggered by log in
    public void sendMessage(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.editText);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        TextView errorIdLengthMsg = findViewById(R.id.error_id_length);
        if (message.length() == 9) {
            startActivity(intent);
            errorIdLengthMsg.setVisibility(View.INVISIBLE);
        } else {
            errorIdLengthMsg.setVisibility(View.VISIBLE);
        }
    }
}