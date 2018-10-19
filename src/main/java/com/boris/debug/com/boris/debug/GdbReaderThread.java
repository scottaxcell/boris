package com.boris.debug.com.boris.debug;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class GdbReaderThread extends Thread {

    private InputStream inputStream;
    static final private String THREAD_NAME = new String("GDB Reader Thread");

    GdbReaderThread(InputStream inputStream) {
        this.inputStream = inputStream;
        setName(THREAD_NAME);
    }

    @Override
    public void run() {
//        super.run();
        // TODO intialize MIParser
        // TODO while loop listening to process stream

        byte[] buffer = new byte[4096];
        int numBytesRead;
        try {
            while ((numBytesRead = inputStream.read(buffer)) != -1) {
                String stringRead = new String(buffer, StandardCharsets.UTF_8);
                System.out.println("SGA: GdbReaderThread read: " + stringRead);

            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
