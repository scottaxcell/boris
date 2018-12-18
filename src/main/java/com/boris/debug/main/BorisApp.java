package com.boris.debug.main;

import com.boris.debug.main.ui.Boris;

public class BorisApp {

    public static void main(String[] args) {
        System.out.println("Yes! I am invincible!");
        new Boris();
    }

    public static void run() {
        Thread thread = new Thread(() -> new Boris());
        thread.start();
    }
}
