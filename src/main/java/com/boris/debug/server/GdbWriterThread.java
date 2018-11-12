package com.boris.debug.server;

import java.io.OutputStream;

public class GdbWriterThread extends Thread {

    private OutputStream outputStream;
    static final private String THREAD_NAME = new String("GDB Writer Thread");
    // TODO queue of commands needed

    GdbWriterThread(OutputStream outputStream) {
        this.outputStream = outputStream;
        setName(THREAD_NAME);
    }

    @Override
    public void run() {
        super.run();
        // TODO wait for command queue to get a command and send it, repeat
    }
}
