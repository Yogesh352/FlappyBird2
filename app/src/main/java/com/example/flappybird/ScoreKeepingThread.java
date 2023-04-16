package com.example.flappybird;

public class ScoreKeepingThread extends Thread {
    private final Game game;
    private volatile boolean running = true;

    public ScoreKeepingThread(Game game) {
        this.game = game;
    }

    @Override
    public void run() {
        while (running) {
                //increments the score when it detects either of these conditions
                if (game.getPassedPipe() || game.getCollideBerry() == 1) {
                        // Update score here
                        int currentScore = game.getScore();
                        currentScore += 1;
                        game.setScore(currentScore);
                        if(game.getCollideBerry() == 1) {
                            game.setBerryCollided(2);
                        }
                        // Notify waiting threads
                        game.setPassedPipe(false);

            } else if (game.getCollideBomb() ==1) {
                    //decrement the score if this condition is satisfied

                    int currentScore = game.getScore();
                    currentScore -= 1;
                    game.setScore(currentScore);
                    if(game.getCollideBomb() == 1) {
                        game.setBombCollided(2);
                    }
                    // Notify waiting threads
                    game.setPassedPipe(false);
                }
        }
    }

    public void stopThread() {
        running = false;
    }
}