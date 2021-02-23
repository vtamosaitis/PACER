package com.tamosaitis.pacer;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class ExerciseActivity extends AppCompatActivity {

    /**
     * ---------------------------------------------------------------------------------------------
     * currentRunTime:  total duration of current run
     * startTime:       the moment the timer starts or resumes
     * sumPastRuns:     total duration of all runs excluding current run
     * correctedTime:   total duration of all runs (MillisecondTime + Time Buff)
     * currentLevel:    current level of workout; resets to startingLevel
     * hasStarted:      tracks if a workout is in progress (including paused workouts)
     *                      used to override onBackPressed to prevent unwanted back button
     *                      presses from ending workout
     *
     * minutes, seconds, milliseconds are displayed in stopwatchTextView as total time elapsed
     *
     * Above values are reset on reset button click
     * ---------------------------------------------------------------------------------------------
     *
     * shuttleDuration: the duration of shuttles on current level
     * milliRemaining:  milliseconds remaining until current shuttle ends
     * startingLevel:   level that the workout will start at (passed from MainActivity)
     * maxLevel:        level that the workout will stop incrementing at; the workout will end at
     *                      this level is isEndless is false (passed from MainActivity)
     * isEndless:       workout will continue until stopped by user if this is true, else
     *                      it will end at endingLevel (passed from Main Activity)
     * ---------------------------------------------------------------------------------------------
     **/

    final long COUNTDOWN_AUDIO_DELAY = 3200L;

    protected TextView stopwatchTextView, levelTextView;
    protected Button start, pause, reset;
    private long currentRunTime, startTime, sumPastRuns, correctedTime = 0L;
    private long shuttleDuration, milliRemaining;
    private int minutes, seconds, milliseconds, shuttlesRemaining;
    private double speedMultiplier = 1; // Intended to be a user setting -- Not yet implemented
    private boolean hasStarted = false;
    private boolean endWorkout = false;
    private float volume = 1.0f;
    private Handler handler;
    private Runnable runnable;
    private Vibrator v;
    private SoundPool cues;
    private int countdownCue, shuttleCue, levelCue, currentLevel, startingLevel, maxLevel;
    private boolean isEndless;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise);

        Bundle extras = getIntent().getExtras();
        startingLevel = extras.getInt("StartingLevel");
        maxLevel = extras.getInt("MaxLevel");
        isEndless = extras.getBoolean("Endless");
        currentLevel = startingLevel;

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            cues = new SoundPool.Builder()
                    .setMaxStreams(3)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            cues = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
        }
        countdownCue = cues.load(this, R.raw.countdown_tone, 1);
        shuttleCue = cues.load(this, R.raw.sublevel_tone, 1);
        levelCue = cues.load(this, R.raw.new_level_tone, 1);

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        final PowerManager.WakeLock wl = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "e_activity:PWL");

        stopwatchTextView = findViewById(R.id.stopwatch_text_view);
        levelTextView = findViewById(R.id.level_text_view);
        start = findViewById(R.id.start);
        pause = findViewById(R.id.pause);
        reset = findViewById(R.id.reset);

        levelTextView.setText("" + currentLevel);
        handler = new Handler();
        levelLogic(startingLevel);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                milliRemaining = shuttleDuration;
                runCountdown();
                
                // start workout after countdown
                startTime = SystemClock.uptimeMillis() + COUNTDOWN_AUDIO_DELAY;
                handler.postDelayed(runnable, COUNTDOWN_AUDIO_DELAY);

                start.setVisibility(View.GONE);
                pause.setVisibility(View.VISIBLE);
                reset.setVisibility(View.GONE);

                hasStarted = true;
                wl.acquire();
            }
        });

        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handler.removeCallbacks(runnable);
                sumPastRuns += currentRunTime;
                currentRunTime = 0;
                cues.autoPause();
                v.cancel();
                wl.release();

                start.setVisibility(View.VISIBLE);
                pause.setVisibility(View.GONE);
                reset.setVisibility(View.VISIBLE);
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                currentRunTime = 0L;
                startTime = 0L;
                sumPastRuns = 0L;
                correctedTime = 0L;
                seconds = 0;
                minutes = 0;
                milliseconds = 0;
                currentLevel = startingLevel;
                hasStarted = false;

                stopwatchTextView.setText("00:00.000");
                levelTextView.setText("" + currentLevel);
                start.setVisibility(View.VISIBLE);
                pause.setVisibility(View.GONE);
                reset.setVisibility(View.GONE);
                levelLogic(currentLevel);
            }
        });

        // Workout Thread
        runnable = new Runnable() {

            public void run() {
                currentRunTime = SystemClock.uptimeMillis() - startTime;
                
                // Convert system time to be readable for the clock
                // Note that the clock uses correctedTime, which
                // is the total time of the entire workout:
                correctedTime = sumPastRuns + currentRunTime;
                seconds = (int) (correctedTime / 1000);
                minutes = seconds / 60;
                seconds = seconds % 60;
                milliseconds = (int) (correctedTime % 1000);

                if (milliRemaining - currentRunTime <= 0) {
                    shuttlesRemaining--;
                    shuttleCheck();
                }

                stopwatchTextView.setText("" +
                        String.format("%02d", minutes) + ":" +
                        String.format("%02d", seconds) + "." +
                        String.format("%03d", milliseconds));


                handler.postDelayed(this, 0);

                if (endWorkout){
                    nextLevel();
                    hasStarted = false;
                    endWorkout = false;
                    reset.setVisibility(View.VISIBLE);
                    start.setVisibility(View.GONE);
                    pause.setVisibility(View.GONE);

                    handler.removeCallbacks(runnable);
                    wl.release();
                }
            }

        };
    }

    private void shuttleCheck() {
        if (shuttlesRemaining > 0) {
            nextShuttle();
        } else {
            if (maxLevel > currentLevel) {
                currentLevel++;
                nextLevel();
                levelTextView.setText("" + currentLevel);
                levelLogic(currentLevel);
            } else if (isEndless) {
                nextLevel();
                levelLogic(currentLevel);
            } else {
                endWorkout = true;
            }
        }
        milliRemaining = currentRunTime + shuttleDuration;
    }

    // Logic to determine the shuttle duration and number of shuttles for a give level.
    private void levelLogic(int level) {
        shuttleDuration = Math.round(72000 / (speedMultiplier * ((level - 1) * 0.5 + 8.5)));
        shuttlesRemaining = (int) (66000 / shuttleDuration);
    }

    // Plays audio/vibration cues when the user starts a new run or resumes a run from pause.
    // Plays audio/vibration depending on the user settings.
    private void runCountdown() {
        final long[] pattern = {0L, 200L, 800L, 200L, 800L, 200L, 800L, 400L};
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("vibration_switch", false)) {
            v.vibrate(pattern, -1);
        }
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("volume_switch", false)) {
            cues.play(countdownCue, volume, volume, 0, 0, 1);
        }
    }
    
    // Plays audio/vibration cues to signal a new level.
    // Plays audio/vibration depending on the user settings.
    private void nextLevel() {
        final long[] pattern = {0L, 200L, 100L, 200L};
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("vibration_switch", false)) {
            v.vibrate(pattern, -1);
        }
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("volume_switch", false)) {
            cues.play(levelCue, volume, volume, 0, 0, 1);
        }
    }

    // Plays audio/vibration cues to signal a new shuttle.
    // Plays audio/vibration depending on the user settings.
    private void nextShuttle() {
        final long[] pattern = {0L, 200L};
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("vibration_switch", false)) {
            v.vibrate(pattern, -1);
        }
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("volume_switch", false)) {
            cues.play(shuttleCue, volume, volume, 0, 0, 1);
        }
    }

    @Override
    public void onBackPressed() {
        if (hasStarted) {
            moveTaskToBack(true);
        } else {
            cues.autoPause();
            cues.release();
            v.cancel();
            Intent intent = new Intent(this, com.tamosaitis.pacer.MainActivity.class);
            startActivity(intent);
            finish();
        }
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
