package com.example.flappybird;

public class ScoreKeepingThread extends Thread {
    private Game game;
    private volatile boolean running = true;

    public ScoreKeepingThread(Game game) {
        this.game = game;
    }

    @Override
    public void run() {
        while (running) {
            synchronized (game.getLock()) {
                if (game.getPassedPipe()) {

                    try {
                        game.getLock().wait();

                        // Update score here
                        int currentScore = game.getScore();
                        currentScore += 1;
                        game.setScore(currentScore);

                        // Notify waiting threads


                    } catch (InterruptedException e) {
                        // Release lock

                    }
                }
                game.setPassedPipe(false);
                game.getLock().lock();
                game.getLock().unlock();
                game.getLock().notifyAll();
            }





        }
    }

    public void stopThread() {
        running = false;
    }
}