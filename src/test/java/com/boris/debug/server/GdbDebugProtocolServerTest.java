package com.boris.debug.server;

import java.util.HashMap;
import java.util.Map;

public class GdbDebugProtocolServerTest {

    @org.junit.Before
    public void setUp() throws Exception {
    }

    @org.junit.After
    public void tearDown() throws Exception {
    }

    @org.junit.Test
    public void launch() {
        Map<String, Object> args = new HashMap<>();
        GdbDebugProtocolServer server = new GdbDebugProtocolServer();
        server.launch(args);
    }

}