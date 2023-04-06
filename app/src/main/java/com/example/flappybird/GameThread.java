package com.example.flappybird;

public class GameThread extends Thread {
    private boolean isRunning = false;

    private final Game game;

    public GameThread(final Game game) {
        this.game = game;
    }

    public void startLoop() {
        isRunning = true;
        start();
    }

    public void stopLoop() {
        isRunning = false;
    }

    @Override
    public void run() {
        super.run();
        while (isRunning) {
            synchronized (game.getLock()) {
                game.getLock().lock();
                try {
//                    // Update score here
//
//                    // Notify waiting threads
                    if(game.passPipe == true) {
                        game.getLock().notifyAll();

                    }
                } finally {
                    // Release lock
                    game.getLock().unlock();
                }
            }
            game.setPassedPipe(false);
            game.draw();


        }
    }

//    private void game_sleep() {
//        long sleepTime = game.getSleepTime();
//        if (sleepTime > 0) {
//            try {
//                sleep(sleepTime);
//            } catch (final Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
}

