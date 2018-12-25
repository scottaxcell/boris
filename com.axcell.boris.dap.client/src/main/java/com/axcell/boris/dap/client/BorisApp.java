package com.axcell.boris.dap.client;

import com.axcell.boris.dap.client.ui.Boris;

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
