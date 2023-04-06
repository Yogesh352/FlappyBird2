package com.example.flappybird;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.text.TextPaint;
import android.view.Display;
import android.view.MotionEvent;

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

    private final Runnable runnable;

    Handler handler;
    Context context;
    final int UPDATE_MILLIS = 30;
    Bitmap background;
    Bitmap topTube, bottomTube;
    Display display;
    Point point;


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

    int gap = 400; //distance between top and bottom tube
    int minTubeOffset, maxTubeOffset;
    int numberOfTubes = 4;
    int distanceBetweenTubes;

    int[] tubeX = new int[numberOfTubes];
    int[] tubeY = new int [numberOfTubes];

    Random random;

    int tubeVelocity = 8;
    int score = 0;
    private ReentrantLock lock;

    boolean passPipe = false;
    int resetCounter = 0;



    public Game(final Runnable runnable, final Predicate<Consumer<Canvas>> useCanvas, Context context) {
        this.runnable = runnable;
        this.useCanvas = useCanvas;
        this.context = context;
        handler = new Handler();
        lock = new ReentrantLock();

        background = BitmapFactory.decodeResource(context. getResources(), R.drawable.background);
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

    }

    public void resize(int width, int height) {
        this.dWidth = width;
        this.dHeight = height;

    }


    public void draw() {
        if (useCanvas.test(this::draw)) {

        }
    }
    private void launchGameOver(){
        handler.removeCallbacksAndMessages(null);
        Intent intent = new Intent(context, GameOver.class);
//        intent.putExtra("Score", score);
        context.startActivity(intent);
        ((Activity) context).finish();
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

        if (gameStart == true) {

            if (birdY < dHeight - birds[0].getHeight() || velocity < 0) {
                velocity += gravity; //faster as it falls
                birdY += velocity;
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
                    if(birdY - birds[birdFrame].getHeight()/6  <= tubeY[i] || birdY + birds[birdFrame].getHeight()/4>= tubeY[i] + gap) {
                        launchGameOver();
                        gameOver = true;

                    }
                }
                if(birdX >=tubeX[i] + topTube.getWidth() + distanceBetweenTubes){
                    resetCounter = 0;
                }
                if(birdX >=tubeX[i] + topTube.getWidth() ){
                    if(resetCounter == 0) {
                        birdPassedPipe();
                        resetCounter++;
                    }

                }

            }

        }

        if(gameStart == false) {
           canvas.drawBitmap(birds[birdFrame], birdX,birdY,null);
            handler.postDelayed(runnable, UPDATE_MILLIS);
        }
        canvas.drawText(Integer.toString(score), xPos, yPos, textPaint);

    }

    public void update() {
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
        if(action == MotionEvent.ACTION_DOWN){
            velocity = -30;
            gameStart = true; //only start upon first click

        }

    }
    public int getScore (){
        return score;
    }
    public void setScore (int score){
        this.score = score;
    }
}

