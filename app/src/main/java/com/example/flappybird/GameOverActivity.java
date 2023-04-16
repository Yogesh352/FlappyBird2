package com.example.flappybird;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class GameOverActivity  extends AppCompatActivity {
    TextView score;
    TextView highScore;

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_over);
        score= findViewById(R.id.score);
        int points = getIntent().getExtras().getInt("score");
        highScore= findViewById(R.id.highscore);
        int highpoints = getIntent().getExtras().getInt("highScore");

        score.setText(""+ points);
        highScore.setText(""+ highpoints);

    }

    public void restart(View view){
        Intent intent = new Intent(GameOverActivity.this, MainActivity.class);
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
