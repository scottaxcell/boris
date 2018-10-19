package com.boris.debug.com.boris.debug;

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
        GdbDebugProtocolServer server = new GdbDebugProtocolServer();
        server.launch(args);
    }

}
