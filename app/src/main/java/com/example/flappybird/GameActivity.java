package com.example.flappybird;

import android.app.Activity;
import android.os.Bundle;


import com.example.flappybird.database.FeedReaderDbHelper;


public class GameActivity extends Activity{

    private final FeedReaderDbHelper dbHelper = new FeedReaderDbHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //hideStatusBar();
        setContentView(new GameView(this, dbHelper));
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
}
