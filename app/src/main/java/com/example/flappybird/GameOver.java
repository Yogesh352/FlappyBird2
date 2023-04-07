package com.example.flappybird;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class GameOver  extends AppCompatActivity {
    TextView score;
    TextView highScore;
    private Game game;

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
        Intent intent = new Intent(GameOver.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        startActivity(intent);
        finish();
    }

    public void exit(View view){
        finish();
    }
}
