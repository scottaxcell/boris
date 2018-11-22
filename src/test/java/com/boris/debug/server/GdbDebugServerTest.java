package com.boris.debug.server;

import org.eclipse.lsp4j.debug.SetBreakpointsArguments;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.SourceBreakpoint;

import java.util.HashMap;
import java.util.Map;

public class GdbDebugServerTest {

    @org.junit.Before
    public void setUp() throws Exception {
    }

    @org.junit.After
    public void tearDown() throws Exception {
    }

    @org.junit.Test
    public void launch() throws InterruptedException {
        Map<String, Object> args = new HashMap<>();
        GdbDebugServer server = new GdbDebugServer();
        server.launch(args);
    }

    @org.junit.Test
    public void setBreakPoints() throws InterruptedException {
        Map<String, Object> args = new HashMap<>();
        args.put("/home/saxcell/dev/boris/testcases/helloworld/helloworld", new Object());
        GdbDebugServer server = new GdbDebugServer();
        server.launch(args);
        Thread.sleep(300);

        Source source = new Source();
        source.setPath("/home/saxcell/dev/boris/testcases/helloworld/helloworld.cpp");
        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(Long.valueOf(9));
        SetBreakpointsArguments setBreakpointsArguments = new SetBreakpointsArguments();
        setBreakpointsArguments.setSource(source);
        setBreakpointsArguments.setBreakpoints(new SourceBreakpoint[] {sourceBreakpoint});
        server.setBreakpoints(setBreakpointsArguments);
        Thread.sleep(300);
    }

}