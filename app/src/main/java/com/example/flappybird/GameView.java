package com.example.flappybird;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.app.Activity;

import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

import android.view.View;
import android.os.Handler;
import android.graphics.Canvas;

import com.example.flappybird.database.FeedReaderDbHelper;

import java.util.Random;
import java.util.function.Consumer;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    //need handler to schedule runnable after delay
    Handler handler;
    Context context;
    Runnable runnable;
    private ScoreKeepingThread scoreKeepingThread;

    private GameThread gameThread;
    private final Game game;

    private FeedReaderDbHelper dbHelper;

//    private void sendNotification() {
//        NotificationPublisher.showNotification(getContext());
//    }

    private boolean useCanvas(final Consumer<Canvas> onDraw) {
        boolean result = false;
        try {
            final SurfaceHolder holder = getHolder();
            final Canvas canvas = holder.lockCanvas();
            try {
                onDraw.accept(canvas);
            } finally {
                try {
                    holder.unlockCanvasAndPost(canvas);
                    result = true;
                } catch (final IllegalStateException e) {
                    // Do nothing
                }
            }
        } catch (final IllegalArgumentException e) {
            e.printStackTrace();
        }
        return result;
    }


    public GameView(Context context, FeedReaderDbHelper dbHelper) {
        super(context);
        handler = new Handler();
        this.context = context;
        this.dbHelper = dbHelper;
        this.game = new Game( this::useCanvas, getContext(), dbHelper);
        runnable = new Runnable() {
            @Override
            public void run() {
                invalidate(); // calls onDraw
            }
        };
        setKeepScreenOn(true);
        getHolder().addCallback(this);
        setFocusable(View.FOCUSABLE);
        setOnTouchListener((view, event) -> {
            game.click(event);
            return true;
        });
    }



    @Override
    protected void onDraw(Canvas canvas){
    }
    @Override
    public void surfaceCreated(final SurfaceHolder surfaceHolder) {
       scoreKeepingThread = new ScoreKeepingThread(game);
       scoreKeepingThread.start();
        if ((gameThread == null) || (gameThread.getState() == Thread.State.TERMINATED)) {
            gameThread = new GameThread(game);
        }
        final Rect rect = getHolder().getSurfaceFrame();
        game.resize(rect.width(), rect.height());
        gameThread.startLoop();
    }

    @Override
    public void surfaceChanged(final SurfaceHolder surfaceHolder, final int format, final int width, final int height) {
        game.resize(width, height);
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder surfaceHolder) {
        scoreKeepingThread.stopThread();
        scoreKeepingThread = null;
        gameThread.stopLoop();
        gameThread = null;
    }

    @Override
    public void draw(final Canvas canvas) {
        super.draw(canvas);
        game.draw();
    }
}
