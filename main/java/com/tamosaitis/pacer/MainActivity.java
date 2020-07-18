package com.tamosaitis.pacer;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    protected Button begin;
    protected EditText startLevelET, maxLevelET;
    protected CheckBox endlessToggleCB;
    private int startLevel = 1;
    private int maxLevel = 25;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        begin = findViewById(R.id.begin_workout_button);
        startLevelET = findViewById(R.id.starting_level_edit_text);
        maxLevelET = findViewById(R.id.max_level_edit_text);
        endlessToggleCB = findViewById(R.id.endless_mode_toggle);

        startLevelET.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int tempLevel;
                if (startLevelET.getText().toString().length() == 0) {tempLevel = 1;}
                else {tempLevel = Integer.parseInt(startLevelET.getText().toString());}
                if(tempLevel < 1) {tempLevel = 1;}
                else if (tempLevel > 30) {tempLevel = 30;}
                startLevel = tempLevel;
                startLevelET.setText("" + startLevel);
                if(startLevel > maxLevel){
                    maxLevel = startLevel;
                    maxLevelET.setText("" + maxLevel);
                }
                maxLevelET.requestFocus();
            }
        });


        maxLevelET.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int tempLevel;
                if (maxLevelET.getText().toString().length() == 0) {tempLevel = 25;}
                else {tempLevel = Integer.parseInt(maxLevelET.getText().toString());}
                if(tempLevel < 1) {tempLevel = 1;}
                else if (tempLevel > 30) {tempLevel = 30;}
                maxLevel = tempLevel;
                maxLevelET.setText("" + maxLevel);
                if(maxLevel < startLevel){
                    startLevel = maxLevel;
                    startLevelET.setText("" + startLevel);
                }
                maxLevelET.clearFocus();
                ((InputMethodManager)MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(maxLevelET.getWindowToken(), 0);
            }
        });

        begin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(startLevelET.getText().toString().length() == 0) startLevel = 1;
                else startLevel = Integer.parseInt(startLevelET.getText().toString());
                if(maxLevelET.getText().toString().length() == 0) maxLevel = 25;
                else maxLevel = Integer.parseInt(maxLevelET.getText().toString());
                Intent intent = new Intent(view.getContext(), ExerciseActivity.class);
                Bundle extras = new Bundle();
                extras.putInt("StartingLevel",startLevel);
                extras.putInt("MaxLevel",maxLevel);
                extras.putBoolean("Endless", endlessToggleCB.isChecked());
                intent.putExtras(extras);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.settings) {
            Intent intent = new Intent(this, com.tamosaitis.pacer.SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

