package com.example.flappybird;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {
    private ToggleButton toggleMusic;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        System.out.println("returning herere????");
        toggleMusic = findViewById(R.id.toggle_music);

        mediaPlayer = MediaPlayer.create(this, R.raw.background_music);
        mediaPlayer.setLooping(true);

        toggleMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                }
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
//        System.out.println("HEllo");

    }


    public void startGame(View view){
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }
}