package com.boris.debug.server;

import org.eclipse.lsp4j.debug.*;

import java.lang.Thread;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class GdbDebugServerTest {

    @org.junit.Before
    public void setUp() throws Exception {
    }

    @org.junit.After
    public void tearDown() throws Exception {
    }

    @org.junit.Test
    public void initialize() throws InterruptedException, ExecutionException {
        GdbDebugServer server = new GdbDebugServer(new Target("/home/saxcell/dev/boris/testcases/helloworld/helloworld"));
        Map<String, Object> args = new HashMap<>();
        server.launch(args);
        Thread.sleep(300);

        Source source = new Source();
        source.setPath("/home/saxcell/dev/boris/testcases/helloworld/helloworld.cpp");
        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(Long.valueOf(9));
        SetBreakpointsArguments setBreakpointsArguments = new SetBreakpointsArguments();
        setBreakpointsArguments.setSource(source);
        setBreakpointsArguments.setBreakpoints(new SourceBreakpoint[] {sourceBreakpoint});
        CompletableFuture<SetBreakpointsResponse> future = server.setBreakpoints(setBreakpointsArguments);
        SetBreakpointsResponse setBreakpointsResponse = future.get();
        Thread.sleep(3000);
    }

    @org.junit.Test
    public void terminate() throws InterruptedException {
        GdbDebugServer server = new GdbDebugServer(new Target("/home/saxcell/dev/boris/testcases/helloworld/helloworld"));
        Map<String, Object> args = new HashMap<>();
        server.launch(args);
        Thread.sleep(300);

        server.terminate(null);
        Thread.sleep(3000);
    }

}