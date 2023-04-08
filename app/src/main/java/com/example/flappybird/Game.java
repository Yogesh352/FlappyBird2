package com.example.flappybird;

import static android.content.ContentValues.TAG;
import static android.content.Context.VIBRATOR_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.text.TextPaint;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.os.Vibrator;
import android.widget.ImageButton;
import android.view.View;

import com.example.flappybird.database.FeedReaderContract;
import com.example.flappybird.database.FeedReaderContract2;
import com.example.flappybird.database.FeedReaderDbHelper;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A class representing the main logic of this demo.
 */
public class Game {

    private final Object mutex = new Object();

    private final Predicate<Consumer<Canvas>> useCanvas;

    private final Paint textPaint = new TextPaint();

    private FeedReaderDbHelper dbHelper;


    Handler handler;
    Context context;
    final int UPDATE_MILLIS = 30;
    Bitmap background;

    Bitmap pause_button;

    Bitmap topTube, bottomTube;
    Display display;
    Point point;
    Vibrator v;


    int dWidth, dHeight;
    Rect rect;
    private ScoreKeepingThread scoreKeepingThread;

    Bitmap[] birds;

    int birdFrame = 0;
    int velocity = 0;
    int gravity = 3;

    int birdX, birdY;

    boolean gameStart = false;
    boolean gameOver = false;
    boolean gamePaused = false;

    int gap = 400; //distance between top and bottom tube
    int minTubeOffset, maxTubeOffset;
    int numberOfTubes = 4;
    int distanceBetweenTubes;

    int[] tubeX = new int[numberOfTubes];
    int[] tubeY = new int [numberOfTubes];

    Random random;

    int tubeVelocity = 12;
    int score = 0;

    private ReentrantLock lock;

    boolean passPipe = false;
    SharedPreferences sharedPreferences;
    int highScore=0;

    private int x;
    private int y;



    public Game(final Predicate<Consumer<Canvas>> useCanvas, Context context, FeedReaderDbHelper dbHelper) {
        this.dbHelper = dbHelper;

        this.useCanvas = useCanvas;
        this.context = context;
        handler = new Handler();
        lock = new ReentrantLock();

        background = BitmapFactory.decodeResource(context. getResources(), R.drawable.background);
        pause_button = BitmapFactory.decodeResource(context.getResources(),R.drawable.pause_button);
        int btnWidth = 250; // set the pause button width
        int btnHeight = 250; // set the pause button height
        pause_button = Bitmap.createScaledBitmap(pause_button, btnWidth, btnHeight, false);

        topTube = BitmapFactory.decodeResource(context.getResources(),R.drawable.top_tube);
        bottomTube = BitmapFactory.decodeResource(context.getResources(),R.drawable.bottom_tube);
        point = new Point();
        dWidth =  context.getResources().getDisplayMetrics().widthPixels;
        dHeight= context.getResources().getDisplayMetrics().heightPixels;
//        dHeight= point.y;
        rect = new Rect(0,0,dWidth,dHeight);


        birds = new Bitmap[2];
        birds[0] = BitmapFactory.decodeResource(context.getResources(), R.drawable.flappy_bird);
        birds[1] = BitmapFactory.decodeResource(context.getResources(), R.drawable.flappy_bird2);

        birdX  = dWidth/2 - birds[0].getWidth()/2; //controls the location by X axis
        birdY = dHeight/2 - birds[0].getHeight()/2;
        v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        distanceBetweenTubes = dWidth*3/4;

        minTubeOffset = gap/2;
        maxTubeOffset= dHeight-minTubeOffset-gap;

        random = new Random();

        for(int i = 0; i < numberOfTubes; i++){
            tubeX[i] = dWidth + i*distanceBetweenTubes;
            tubeY[i] = minTubeOffset + random.nextInt(maxTubeOffset - minTubeOffset + 1);
        }
        {
            textPaint.setAntiAlias(true);
            textPaint.setTextSize(30 * context.getResources().getDisplayMetrics().density);
            textPaint.setColor(0xFF000000);
            textPaint.setFakeBoldText(true);
        }

        sharedPreferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
         highScore= sharedPreferences.getInt("high_score", 0);
        System.out.println(highScore);
    }

    public void resize(int width, int height) {
        this.dWidth = width;
        this.dHeight = height;

    }


    public void draw() {
        if (useCanvas.test(this::draw)) {

        }
    }

    public Vibrator getVibrator(){
        return v;
    }

    @SuppressLint("DefaultLocale")
    private void draw(Canvas canvas) {

        if (canvas == null) {
            return;
        }
        canvas.drawBitmap(background,null,rect,null);

        int xPos = (canvas.getWidth() / 2);
        int yPos = (int) ((canvas.getHeight() / 4)) ;

        if(birdFrame == 0){
            birdFrame  = 1;
        } else{
            birdFrame = 0;
        }

        if (gameStart == true && gameOver == false) {

            if (birdY < dHeight - birds[0].getHeight() || velocity < 0) {
                velocity += gravity; //faster as it falls
                birdY += velocity;
            } else{
                gameOver = true;
                launchGameOver();
            }
            for(int i = 0 ; i < numberOfTubes; i++) {
                tubeX[i] -=tubeVelocity;
                if(tubeX[i] < -topTube.getWidth()){
                    tubeX[i] += numberOfTubes * distanceBetweenTubes;
                    tubeY[i] = minTubeOffset + random.nextInt(maxTubeOffset - minTubeOffset + 1);

                }
                canvas.drawBitmap(birds[birdFrame], birdX,birdY,null);

                canvas.drawBitmap(topTube, tubeX[i], tubeY[i] - topTube.getHeight(), null);
                canvas.drawBitmap(bottomTube, tubeX[i], tubeY[i] + gap, null);
                if(birdX>=tubeX[i] - topTube.getWidth()/2 && birdX <=tubeX[i] + topTube.getWidth()){
                    //adjust to make the contact thingy better
//                    System.out.println("height of bird is " + birds[birdFrame].getHeight());
//                    System.out.println("bird Y is " + birdY);
//                    int trial = tubeY[i]+gap;
//                    System.out.println("Tube Y is " +trial);
                    if(birdY - birds[birdFrame].getHeight()/6  <= tubeY[i] || birdY + birds[birdFrame].getHeight()>= tubeY[i] + gap) {
                        gameOver = true;
                        long[] pattern = {0, 100, 1000};
                        v.vibrate(pattern, 0);
                        try{Thread.sleep(100);}catch(InterruptedException e){}
                        v.cancel();
                        launchGameOver();
                    }
                }

//                int trial = tubeX[i] + topTube.getWidth();
//                System.out.println("birdX is " +  birdX);
//                System.out.println("distance is " + trial);


                if(birdX >=tubeX[i] + topTube.getWidth() -5 && birdX <=tubeX[i] + topTube.getWidth() + 10){
                        birdPassedPipe();
                }

            }

        }

        if(gameStart == false) {
           canvas.drawBitmap(birds[0], birdX,birdY,null);
//            handler.postDelayed(runnable, UPDATE_MILLIS);
        }
        canvas.drawText(Integer.toString(score), xPos, yPos, textPaint);
        canvas.drawBitmap(pause_button, 0, 0, null);

    }

    public void update() {
    }
    private void launchGameOver(){
        handler.removeCallbacksAndMessages(null);
        if (score > highScore){
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("high_score", score);
            editor.apply();
        }
        Intent intent = new Intent(context, GameOver.class);
        intent.putExtra("score", score);
        if(score > highScore) {
            intent.putExtra("highScore", score);
        }else{
            intent.putExtra("highScore", highScore);
        }

        String player = "Player";
        //adding score into database
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE, player);
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_SUBTITLE, score);
        long newRowId = db.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);
//        System.out.println("new record " + newRowId);

        String[] selectionArgs = { player };
        String selection = FeedReaderContract2.FeedEntry.COLUMN_NAME_TITLE + " LIKE ?";
        String countQuery = "SELECT COUNT(*) FROM " + FeedReaderContract2.FeedEntry.TABLE_NAME +
                " WHERE " + selection;
        Cursor cursor = db.rawQuery(countQuery, selectionArgs);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();

        values = new ContentValues();
        values.put(FeedReaderContract2.FeedEntry.COLUMN_NAME_TITLE, player);
        values.put(FeedReaderContract2.FeedEntry.COLUMN_NAME_SUBTITLE, highScore);
        //update high score in databasae
        if (count > 0) {
            int updated_rows = db.update(
                    FeedReaderContract2.FeedEntry.TABLE_NAME,
                    values,
                    selection,
                    selectionArgs);
            System.out.println("new highscore " + updated_rows);
        } else {
            newRowId = db.insert(FeedReaderContract2.FeedEntry.TABLE_NAME, null, values);
        }


        context.startActivity(intent);
        ((Activity) context).finish();
    }

    private void launchGamePause(){
        handler.removeCallbacksAndMessages(null);
        Intent intent = new Intent(context, GamePause.class);
        context.startActivity(intent);
        //((Activity) context).finish();
    }

    public void birdPassedPipe() {
        passPipe = true;
    }

    public boolean getPassedPipe (){
        return passPipe;
    }

    public void setPassedPipe (Boolean passPipe){
        this.passPipe = passPipe;
    }

    public ReentrantLock getLock(){
        return lock;
    }

    public void click(MotionEvent event) {
        int action = event.getAction();
//        if(action == MotionEvent.ACTION_DOWN){
//            velocity = -30;
//            gameStart = true; //only start upon first click
//
//        }

        if (action == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();
            // check if the touch event is inside the pause button
            if (x >= 0 && x <= pause_button.getWidth() && y >= 0 && y <= pause_button.getHeight()) {
                gamePaused = !gamePaused; // toggle variable
                if (gamePaused) {
                    Log.d("Game", "Game paused");
                    // handle pause button click
                    gamePaused = !gamePaused;
                    launchGamePause();
                }
            } else {
                // handle non-pause button click
                velocity = -30;
                gameStart = true; //only start upon first click
            }
        }

    }
    public void setPaused(boolean paused) {
        gamePaused = paused;
    }

    public int getScore (){
        return score;
    }
    public void setScore (int score){
        this.score = score;
    }

    public int getHighScore (){
        return  highScore;
    }

    public void setHighScore(int highScore){
        this.highScore = highScore;
    }

}

