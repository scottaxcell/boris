package com.boris.debug.server;

import com.boris.debug.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class GdbReaderThread extends Thread {
    /**
     * Use this as a guide: org.eclipse.cdt.dsf.mi.service.command.AbstractMIControl.RxThread
     */

    private InputStream inputStream;
    static final private String THREAD_NAME = new String("GDB Reader Thread");

    GdbReaderThread(InputStream inputStream) {
        this.inputStream = inputStream;
        setName(THREAD_NAME);
    }

    @Override
    public void run() {
        // TODO intialize MIParser
        // TODO while loop listening to process stream

        byte[] buffer = new byte[4096];
        int numBytesRead;
        try {
            while ((numBytesRead = inputStream.read(buffer)) != -1) {
                String stringRead = new String(buffer, StandardCharsets.UTF_8);
                Utils.debug(this.getClass().getSimpleName() + " -- read " + stringRead);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
