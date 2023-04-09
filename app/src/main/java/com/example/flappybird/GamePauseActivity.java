package com.example.flappybird;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class GamePauseActivity extends AppCompatActivity {
    private Game game;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_pause);

    }
    public void resume(View view) {
        Intent intent = new Intent(GamePauseActivity.this, GameActivity.class);
        //return to the old state and not create a new gameactivity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);

        finish();
    }

    public void restart(View view){
        Intent intent = new Intent(GamePauseActivity.this, MainActivity.class);
        //flags so that the application returns to the initial state of the mainactivity and a new
        //main activity is not created
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        startActivity(intent);
        finish();
    }

    public void exit(View view){
        finish();
    }
}