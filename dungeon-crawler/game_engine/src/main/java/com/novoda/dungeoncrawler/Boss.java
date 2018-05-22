package com.novoda.dungeoncrawler;

class Boss {
    public int position;
    private int lives;
    private boolean alive;

    void spawn() {
        position = 800;
        lives = 3;
        alive = true;
    }

    void hit() {
        lives--;
        if (lives == 0) {
            kill();
            return;
        }
        if (lives == 2) {
            position = 200;
        } else if (lives == 1) {
            position = 600;
        }
    }

    boolean isAlive() {
        return alive;
    }

    void kill() {
        alive = false;
    }

    public int getSpeed() {
        if (lives == 2) {
            return 2000;
        }
        if (lives == 1) {
            return 1500;
        }
        return 2500;
    }
}
