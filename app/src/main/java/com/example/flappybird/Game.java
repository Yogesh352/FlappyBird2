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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
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

import java.lang.reflect.Array;
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
    Bitmap bomb;

    Bitmap topTube, bottomTube;
    Bitmap berry;
    Display display;
    Point point;
    Vibrator v;


    int dWidth, dHeight;
    Rect rect;
    private ScoreKeepingThread scoreKeepingThread;

    Bitmap[] birds;

    int birdFrame = 0;
    int berryFrame = 0;
    int bombFrame = 0;
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
    int[] berryX;
    int[] berryY;

    int[] bombX;
    int[] bombY;



    Random random;

    int tubeVelocity = 12;
    int score = 0;
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            //Do something after 100ms
        }
    };

    private ReentrantLock scorelock;

    boolean passPipe = false;
    SharedPreferences sharedPreferences;
    int highScore=0;

    private int x;
    private int y;

    private int hasCollidedBerry;
    private int hasCollidedBomb;



    public Game(final Predicate<Consumer<Canvas>> useCanvas, Context context, FeedReaderDbHelper dbHelper) {
        this.dbHelper = dbHelper;

        this.useCanvas = useCanvas;
        this.context = context;
        handler = new Handler();
        scorelock = new ReentrantLock();

        background = BitmapFactory.decodeResource(context. getResources(), R.drawable.background);
        pause_button = BitmapFactory.decodeResource(context.getResources(),R.drawable.pause_button);
        int btnWidth = 250; // set the pause button width
        int btnHeight = 250; // set the pause button height
        pause_button = Bitmap.createScaledBitmap(pause_button, btnWidth, btnHeight, false);

        topTube = BitmapFactory.decodeResource(context.getResources(),R.drawable.top_tube);
        bottomTube = BitmapFactory.decodeResource(context.getResources(),R.drawable.bottom_tube);
        berry = BitmapFactory.decodeResource(context.getResources(),R.drawable.berry);
        bomb = BitmapFactory.decodeResource(context.getResources(),R.drawable.bomb);

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
        berryX = random.ints(4, 390, 450).toArray();
        berryY = random.ints(4, 200, 400).toArray();

        bombX = random.ints(4, 390, 550).toArray();
        bombY = random.ints(4, 100, 1000).toArray();


        {
            textPaint.setAntiAlias(true);
            textPaint.setTextSize(30 * context.getResources().getDisplayMetrics().density);
            textPaint.setColor(0xFF000000);
            textPaint.setFakeBoldText(true);
        }
        sharedPreferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
         highScore= sharedPreferences.getInt("high_score", 0);

    }

    public void resize(int width, int height) {
        this.dWidth = width;
        this.dHeight = height;

    }


    public void draw() {
        if (useCanvas.test(this::draw)) {

        }
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

        if(berryFrame == 1){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    berryFrame  = 0;
                }
            }, 50);
        } else if(berryFrame == 0){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    berryFrame  = 1;
                }
            }, 50);
        }

        if(bombFrame == 1){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bombFrame  = 0;
                }
            }, 50);
        } else if(bombFrame == 0){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bombFrame  = 1;
                }
            }, 50);
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

                if( i % 2 == 0 ) {
                    if(berryFrame == 1) {
                        if(hasCollidedBerry == 0) {
                            canvas.drawBitmap(berry, tubeX[i] + berryX[i], tubeY[i] + berryY[i], null);
                        }
                    }else if (berryFrame == 0) {
                        if(hasCollidedBerry == 0) {
                            canvas.drawBitmap(berry, tubeX[i] + berryX[i], tubeY[i] + berryY[i] - 20, null);
                        }
                    }
                    if((birdX>=tubeX[i] + berryX[i] - berry.getWidth()/2 && birdX <=tubeX[i] + berryX[i] + berry.getWidth())){
                        if(berryFrame == 0 && hasCollidedBerry == 0) {
                            if (birdY - birds[birdFrame].getHeight() <= tubeY[i] + berryY[i] && birdY + birds[birdFrame].getHeight() >= tubeY[i] + berryY[i]) {
                                synchronized (mutex) {
                                    birdCollideBerry();
                                }
                            }
                        }else if(berryFrame == 1 && hasCollidedBerry == 0){
                            if(birdY-birds[birdFrame].getHeight() <= tubeY[i] + berryY[i] - 20  && birdY+birds[birdFrame].getHeight() >= tubeY[i] + berryY[i] - 20){
                                synchronized (mutex) {
                                    birdCollideBerry();
                                }
                            }
                        }
                    }
                }

                if( i % 2 == 0) {
                    if(bombFrame == 1) {
                        if(hasCollidedBomb == 0) {
                            canvas.drawBitmap(bomb, tubeX[i] + bombX[i], tubeY[i] + bombY[i], null);
                        }
                    }else if (bombFrame == 0) {
                        if(hasCollidedBomb == 0) {
                            canvas.drawBitmap(bomb, tubeX[i] + bombX[i], tubeY[i] + bombY[i] - 20, null);
                        }
                    }
                    if((birdX>=tubeX[i] + bombX[i] - bomb.getWidth()/2 && birdX <=tubeX[i] + bombX[i] + bomb.getWidth())){
                        if(bombFrame == 0 && hasCollidedBomb == 0) {
                            if (birdY - birds[birdFrame].getHeight() <= tubeY[i] + bombY[i] && birdY + birds[birdFrame].getHeight() >= tubeY[i] + bombY[i]) {
                                synchronized (mutex) {
                                    birdCollideBomb();
                                }
                            }
                        }else if(bombFrame == 1 && hasCollidedBomb == 0){
                            if(birdY-birds[birdFrame].getHeight() <= tubeY[i] + bombY[i] - 20  && birdY+birds[birdFrame].getHeight() >= tubeY[i] + bombY[i] - 20){
                                synchronized (mutex) {
                                    birdCollideBomb();
                                }
                            }
                        }
                    }
                }



                if(birdX>=tubeX[i] - topTube.getWidth()/2 && birdX <=tubeX[i] + topTube.getWidth()){
                    //adjust to make the contact thingy better
                    if(birdY - birds[birdFrame].getHeight()/6  <= tubeY[i] || birdY + birds[birdFrame].getHeight()>= tubeY[i] + gap) {
                        gameOver = true;
                        long[] pattern = {0, 100, 1000};
                        System.out.println("HWERLRELEA");
                        v.vibrate(pattern, 0);
                      try{Thread.sleep(300);}catch(InterruptedException e){}

                      v.cancel();

                        launchGameOver();
                    }
                }

                if(birdX >=tubeX[i] + topTube.getWidth() -5 && birdX <=tubeX[i] + topTube.getWidth() + 10){
                    hasCollidedBerry = 0;
                    hasCollidedBomb = 0;
                    synchronized (mutex) {
                        birdPassedPipe();
                    }
                }

            }
        }

        if(gameStart == false) {
           canvas.drawBitmap(birds[0], birdX,birdY,null);
           handler.postDelayed(runnable, UPDATE_MILLIS);
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
    public void birdCollideBerry() {hasCollidedBerry = 1;}
    public void birdCollideBomb() {hasCollidedBomb = 1;}


    public boolean getPassedPipe (){
        return passPipe;
    }
    public int getCollideBerry (){
        return hasCollidedBerry;
    }

    public int getCollideBomb (){
        return hasCollidedBomb;
    }



    public void setPassedPipe (Boolean passPipe){
        this.passPipe = passPipe;
    }
    public void setBerryCollided (int hasCollidedBerry){
        this.hasCollidedBerry = hasCollidedBerry;
    }
    public void setBombCollided (int hasCollidedBomb){
        this.hasCollidedBomb = hasCollidedBomb;
    }


    public ReentrantLock getScoreLock(){
        return scorelock;
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

