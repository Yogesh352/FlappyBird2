package com.example.flappybird;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ToggleButton;

import com.example.flappybird.database.FeedReaderDbHelper;

public class MainActivity extends AppCompatActivity {
    private ToggleButton toggleMusic;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        //button that controls the music playing in the background
        toggleMusic = findViewById(R.id.toggle_music);

        mediaPlayer = MediaPlayer.create(this, R.raw.background_music);
        mediaPlayer.setLooping(true);

        //upon a click the music will turn on and off accordingly
        toggleMusic.setOnClickListener(v -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        });


    }
    //a onResume function is implemented so that the mediaPlayer is not created again and they can turn off
    //the music when they return to the main page
    @Override
    protected void onResume() {
        super.onResume();
    }

    //Open the GameActivity upon clicking on the play button
    public void startGame(View view){
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }


}