package com.boris.debug;

import com.boris.debug.server.GdbDebugServer;

import java.util.HashMap;
import java.util.Map;

public class Tests {

    @org.junit.Before
    public void setUp() throws Exception {
    }

    @org.junit.After
    public void tearDown() throws Exception {
    }

    @org.junit.Test
    public void gdbLauncher() {
        Map<String, Object> args = new HashMap<>();
        GdbDebugServer server = new GdbDebugServer();
        server.launch(args);
    }

}
