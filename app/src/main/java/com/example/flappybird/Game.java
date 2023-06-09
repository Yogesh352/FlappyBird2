package com.example.flappybird;

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

import android.graphics.Paint;

import android.graphics.Rect;
import android.os.Handler;
import android.text.TextPaint;
import android.util.Log;
import android.view.MotionEvent;
import android.os.Vibrator;



import com.example.flappybird.database.FeedReaderContract;
import com.example.flappybird.database.FeedReaderContract2;
import com.example.flappybird.database.FeedReaderDbHelper;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A class representing the main logic of this demo.
 */
public class Game {
    //mutex to control the updating of the score. There is a need for the lock because there
    // are various aspects of the game that could affect the score. For example, going past a pipe
    // or colliding with a berry would increment the score by 1. Meanwhile, a collision with the
    // bomb will result in the score reducing by 1. There might be  instances where the bird
    // collides with the berry and the bomb at once. Thus, a lock is necessary to prevent
    // the shared resource of score being updated at the same time.
    private final Object mutex = new Object();

    private final Predicate<Consumer<Canvas>> useCanvas;

    //textpaint for the score
    private final Paint textPaint = new TextPaint();


    private final FeedReaderDbHelper dbHelper;


    private final Handler handler;
    private final Context context;
    private final int UPDATE_MILLIS = 30;

    //bitmaps to draw the various elements shown on the screen itself
    private final Bitmap background;

    private Bitmap pause_button;
    private final Bitmap bomb;

    private final Bitmap topTube, bottomTube, topTube2, bottomTube2;
    private final Bitmap berry;

    private final Bitmap[] birds;

    //Controls the vibration upon collision
    private final Vibrator v;
    private final static int targetFps = 30;

    private final static long intervalFps = 1000L;

    private final ElapsedTimer elapsedTimer = new ElapsedTimer();

    private final DeltaStepper fpsUpdater = new DeltaStepper(intervalFps, this::fpsUpdate);

    //screen width and height
    int dWidth, dHeight;
    private final Rect rect;

    //birdFrame controls the flapping of the bird
    private int birdFrame = 0;

    //berryFrame and bombFrame creates the bouncing effect of the elements
    private int berryFrame = 0;
    private int bombFrame = 0;

    //velocity and gravity of the bird
    private int velocity = 0;
    private final int gravity = 3;

    //X and Y coordinates of the bird itself
    private final int birdX;
    private int birdY;

    //variables to track the state of the game
    private boolean gameStart = false;
    private boolean gameOver = false;
    private boolean gamePaused = false;

    //Tube properties
    private final int gap = 400;
    private final int minTubeOffset, maxTubeOffset;
    private final int numberOfTubes = 4;
    private final int tubeVelocity = 12;

    private final int distanceBetweenTubes;

    //creating the X and Y coordinates of the tubes and re-rendering it using a for loop
    private final int[] tubeX = new int[numberOfTubes];
    private final int[] tubeY = new int [numberOfTubes];

    //control the location of the berries and the bombs
    private final int[] berryX;
    private final int[] berryY;

    private final int[] bombX;
    private final int[] bombY;


    private final Random random;

    //Keep track of the score
    private int score = 0;
    private final Runnable runnable = () -> {};

    //Boolean to check if the bird has passed the pipe so that the score can be incremented
    private boolean passPipe = false;

    //SharedPreferences is used to store the state of the high score.
    private final SharedPreferences sharedPreferences;
    private final int highScore;

    //Check for collision with berries and bombs
    private int hasCollidedBerry;
    private int hasCollidedBomb;

    //used to change the color of the pipe every 5 seconds
    private int currentTube = 1;
    private final Timer timer;


    public Game(final Predicate<Consumer<Canvas>> useCanvas, Context context, FeedReaderDbHelper dbHelper) {
        this.dbHelper = dbHelper;

        this.useCanvas = useCanvas;
        this.context = context;
        handler = new Handler();

        //draw out the various elements required for the gameplay
        background = BitmapFactory.decodeResource(context. getResources(), R.drawable.background);
        pause_button = BitmapFactory.decodeResource(context.getResources(),R.drawable.pause_button);

        int btnWidth = 250; // set the pause button width
        int btnHeight = 250; // set the pause button height
        pause_button = Bitmap.createScaledBitmap(pause_button, btnWidth, btnHeight, false);

        topTube = BitmapFactory.decodeResource(context.getResources(),R.drawable.top_tube);
        bottomTube = BitmapFactory.decodeResource(context.getResources(),R.drawable.bottom_tube);
        topTube2 = BitmapFactory.decodeResource(context.getResources(),R.drawable.top_tube2);
        bottomTube2 = BitmapFactory.decodeResource(context.getResources(),R.drawable.bottom_tube2);

        berry = BitmapFactory.decodeResource(context.getResources(),R.drawable.berry);
        bomb = BitmapFactory.decodeResource(context.getResources(),R.drawable.bomb);
        //get display height and width
        dWidth =  context.getResources().getDisplayMetrics().widthPixels;
        dHeight= context.getResources().getDisplayMetrics().heightPixels;
        rect = new Rect(0,0,dWidth,dHeight);



        birds = new Bitmap[2];
        //wings up
        birds[0] = BitmapFactory.decodeResource(context.getResources(), R.drawable.flappy_bird);
        //wings down
        birds[1] = BitmapFactory.decodeResource(context.getResources(), R.drawable.flappy_bird2);

        //starting position of the bird will be in the middle of the screen
        birdX  = dWidth/2 - birds[0].getWidth()/2;
        birdY = dHeight/2 - birds[0].getHeight()/2;
        v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        //setting the distance between the tubes
        distanceBetweenTubes = dWidth*3/4;

        minTubeOffset = gap/2;
        maxTubeOffset= dHeight-minTubeOffset-gap;

        random = new Random();

        //generating the initial X and Y coordinates of the tubes
        for(int i = 0; i < numberOfTubes; i++){
            tubeX[i] = dWidth + i*distanceBetweenTubes;
            tubeY[i] = minTubeOffset + random.nextInt(maxTubeOffset - minTubeOffset + 1);
        }

        //randomly generate the X and Y coordinates of the berries and bombs
        berryX = random.ints(4, 390, 420).toArray();
        berryY = random.ints(4, 200, 400).toArray();

        bombX = random.ints(4, 390, 480).toArray();
        bombY = random.ints(4, 100, 1000).toArray();

        {
            textPaint.setAntiAlias(true);
            textPaint.setTextSize(30 * context.getResources().getDisplayMetrics().density);
            textPaint.setColor(0xFF000000);
            textPaint.setFakeBoldText(true);
        }

        //Keep track of the high score and store it
        sharedPreferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        highScore= sharedPreferences.getInt("high_score", 0);

        //A timer that will result in the change of color every 5 seconds
        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                currentTube = (currentTube + 1)%2;
            }
        }, 0, 5000);

    }
    private boolean fpsUpdate(long deltaTime) {
        final double fractionTime = intervalFps / (double)deltaTime;
        return false;
    }

    public void resize(int width, int height) {
        this.dWidth = width;
        this.dHeight = height;
    }

    public void draw() {
        if (useCanvas.test(this::draw)) {}
    }

    @SuppressLint("DefaultLocale")
    private void draw(Canvas canvas) {

        if (canvas == null) {
            return;
        }
        canvas.drawBitmap(background,null,rect,null);

        int xPos = (canvas.getWidth() / 2);
        int yPos = (int) ((canvas.getHeight() / 4)) ;

        //flapping effect of the bird
        if(birdFrame == 0){
            birdFrame  = 1;
        } else{
            birdFrame = 0;
        }

        //bouncing effect of the berries and bombs
        if(berryFrame == 1){
            handler.postDelayed(() -> berryFrame  = 0, 50);
        } else if(berryFrame == 0){
            handler.postDelayed(() -> berryFrame  = 1, 50);
        }

        if(bombFrame == 1){
            handler.postDelayed(() -> bombFrame  = 0, 50);
        } else if(bombFrame == 0){
            handler.postDelayed(() -> bombFrame  = 1, 50);
        }




        if (gameStart && !gameOver) {
            if (birdY < dHeight - birds[0].getHeight() || velocity < 0) {
                //increment the velocity as it falls
                velocity += gravity;
                birdY += velocity;
            } else{
                //when it hits the floor game ends
                gameOver = true;
                launchGameOver();
            }
            //controls the generation of tubes, berries, bombs and controls the collision with these items
            for(int i = 0 ; i < numberOfTubes; i++) {
                //tube generation and constant rendering
                tubeX[i] -= tubeVelocity;
                if (tubeX[i] < -topTube.getWidth()) {
                    tubeX[i] += numberOfTubes * distanceBetweenTubes;
                    tubeY[i] = minTubeOffset + random.nextInt(maxTubeOffset - minTubeOffset + 1);
                }

                canvas.drawBitmap(birds[birdFrame], birdX, birdY, null);

                //Change of color for the tubes which is controlled by the currentTube variable that alternates every 5 seconds
                if (currentTube == 0){
                    canvas.drawBitmap(topTube, tubeX[i], tubeY[i] - topTube.getHeight(), null);
                } else{
                    canvas.drawBitmap(topTube2, tubeX[i], tubeY[i] - topTube.getHeight(), null);
                }
                if(currentTube == 0) {
                    canvas.drawBitmap(bottomTube, tubeX[i], tubeY[i] + gap, null);
                } else{
                    canvas.drawBitmap(bottomTube2, tubeX[i], tubeY[i] + gap, null);
                }

                //Generation of berries and bombs at every other tube
                if( i % 2 == 0 ) {
                    if(berryFrame == 1) {
                        //draw if the berry has not been collided with
                        if(hasCollidedBerry == 0) {
                            canvas.drawBitmap(berry, tubeX[i] + berryX[i], tubeY[i] + berryY[i], null);
                        }
                    } else if (berryFrame == 0) {
                        if(hasCollidedBerry == 0) {
                            canvas.drawBitmap(berry, tubeX[i] + berryX[i], tubeY[i] + berryY[i] - 20, null);
                        }
                    }
                    //checks if the coordinates of the bird and the berries overlap. An overlap would indicate a collision
                    if((birdX>=tubeX[i] + berryX[i] - berry.getWidth()/2 && birdX <=tubeX[i] + berryX[i] + berry.getWidth())){
                        if(berryFrame == 0 && hasCollidedBerry == 0) {
                            if (birdY - birds[birdFrame].getHeight() <= tubeY[i] + berryY[i] && birdY + birds[birdFrame].getHeight() >= tubeY[i] + berryY[i]) {
                                //grab the lock if available to update the score through the scoreKeepingThread
                                synchronized (mutex) {
                                    birdCollideBerry();
                                }
                            }
                        } else if(berryFrame == 1 && hasCollidedBerry == 0){
                            //same idea as the previous but handles when the berry is in the other frame
                            if(birdY-birds[birdFrame].getHeight() <= tubeY[i] + berryY[i] - 20  && birdY+birds[birdFrame].getHeight() >= tubeY[i] + berryY[i] - 20){
                                synchronized (mutex) {
                                    birdCollideBerry();
                                }
                            }
                        }
                    }
                }

                //performs the same as the code for berries. However, a collision with a bomb will result in the reduction of the score by 1
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


                //Controls the collision of the bird with the tube
                if(birdX>=tubeX[i] - topTube.getWidth()/2 && birdX <=tubeX[i] + topTube.getWidth()){
                    if(birdY - birds[birdFrame].getHeight()/6  <= tubeY[i] || birdY + birds[birdFrame].getHeight()>= tubeY[i] + gap) {
                        gameOver = true;
                        //the device vibrates when the bird collides with the tube
                        long[] pattern = {0, 100, 1000};
                        v.vibrate(pattern, 0);
                        try{Thread.sleep(300);}catch(InterruptedException e){}
                        v.cancel();
                        //function to launch the game over page
                        launchGameOver();
                    }
                }

                //Check if the coordinates of the bird is past the pipe and if so it can grab the mutex and increment the score
                if(birdX >=tubeX[i] + topTube.getWidth() -5 && birdX <=tubeX[i] + topTube.getWidth() + 10){
                    hasCollidedBerry = 0;
                    hasCollidedBomb = 0;
                    synchronized (mutex) {
                        birdPassedPipe();
                    }
                }

            }
        }

        //the initial view before the user taps on the screen to start the game
        if(!gameStart) {
           canvas.drawBitmap(birds[0], birdX,birdY,null);
           handler.postDelayed(runnable, UPDATE_MILLIS);
        }

        //the score as well as the pause button displayed on the screen
        canvas.drawText(Integer.toString(score), xPos, yPos, textPaint);
        canvas.drawBitmap(pause_button, 0, 0, null);

    }

    public long getSleepTime() {
        final double targetFrameTime = (1000.0 / targetFps);
        final long updateEndTime = System.currentTimeMillis();
        final long updateTime = updateEndTime - elapsedTimer.getUpdateStartTime();
        return Math.round(targetFrameTime - updateTime);
    }

    public void update() {
        final long deltaTime = elapsedTimer.progress();
        if (deltaTime <= 0) {
            return;
        }
        fpsUpdater.update(deltaTime);
    }

    //function the handles when the game is over. When the bird collides with the pipe or touches the floor
    private void launchGameOver(){
        handler.removeCallbacksAndMessages(null);

        //update the high score if necessary
        if (score > highScore){
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("high_score", score);
            editor.apply();
        }
        //send both the score and the high score to the game over screen
        Intent intent = new Intent(context, GameOverActivity.class);
        intent.putExtra("score", score);
        if(score > highScore) {
            //send new high score if the old one has been beaten
            intent.putExtra("highScore", score);
        }else{
            //send old high score
            intent.putExtra("highScore", highScore);
        }

        String player = "Player";
        //adding score into database
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_TITLE, player);
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_SUBTITLE, score);
        long newRowId = db.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);

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

        //update high score in database
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

    //function to handle when the pause button is clicked
    private void launchGamePause(){
        handler.removeCallbacksAndMessages(null);
        Intent intent = new Intent(context, GamePauseActivity.class);
        context.startActivity(intent);
    }

    //function the different collisions and events that affect the score
    //these are then checked constantly in the scoreKeepingThread using the getter functions to update the score accordingly
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

    //handles the click event
    public void click(MotionEvent event) {
        int action = event.getAction();

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
                // handle non-pause button click. Velocity decreases as the bird will move up
                velocity = -30;
                //only start the game upon first click
                gameStart = true;
            }
        }

    }

    //getters and setters for the scorekeepingthread to maintain.
    public int getScore (){
        return score;
    }
    public void setScore (int score){
        this.score = score;
    }

}

