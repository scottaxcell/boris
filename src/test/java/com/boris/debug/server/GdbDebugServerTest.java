package com.boris.debug.server;

import org.eclipse.lsp4j.debug.Capabilities;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.junit.Assert;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.*;

public class GdbDebugServerTest {
    private static final int TIMEOUT = 2000;
    private static final int HALF_SECOND = 500;

    private static final String TARGET_FILENAME = "/home/saxcell/dev/boris/testcases/helloworld/helloworld";
    private Target target = new Target(TARGET_FILENAME);

    private GdbDebugServer server;
    private Launcher<IDebugProtocolClient> serverLauncher;
    private Future<?> serverListening;

    private AssertingEndpoint client;
    private Launcher<IDebugProtocolServer> clientLauncher;
    private Future<?> clientListening;

    @org.junit.Before
    public void setUp() throws Exception {
        PipedInputStream inClient = new PipedInputStream();
        PipedOutputStream outClient = new PipedOutputStream();
        PipedInputStream inServer = new PipedInputStream();
        PipedOutputStream outServer = new PipedOutputStream();

        inClient.connect(outServer);
        outClient.connect(inServer);

        server = new GdbDebugServer(target);
        serverLauncher = DSPLauncher.createServerLauncher(server, inServer, outServer);
        serverListening = serverLauncher.startListening();

        client = new AssertingEndpoint();
        server.setRemoteProxy(client);
        clientLauncher = DSPLauncher.createClientLauncher(ServiceEndpoints.toServiceObject(client, IDebugProtocolClient.class), inClient, outClient);
        clientListening = clientLauncher.startListening();
    }

    @org.junit.After
    public void tearDown() throws Exception {
        clientListening.cancel(true);
        serverListening.cancel(true);
        Thread.sleep(10);
    }

    @org.junit.Test
    public void initialize() throws InterruptedException, ExecutionException, TimeoutException {
        InitializeRequestArguments request = new InitializeRequestArguments();
        request.setClientID("com.boris.debug");
        request.setAdapterID("adapterId");
        request.setPathFormat("path");
        request.setSupportsVariableType(true);
        request.setSupportsVariablePaging(true);
        request.setLinesStartAt1(true);
        request.setColumnsStartAt1(true);
        request.setSupportsRunInTerminalRequest(true);

        Capabilities result = new Capabilities();
        result.setSupportsFunctionBreakpoints(false);
        result.setSupportsConditionalBreakpoints(false);

        CompletableFuture<?> future = server.initialize(request);
        Assert.assertEquals(result.toString(), future.get(TIMEOUT, TimeUnit.MILLISECONDS).toString());

        Thread.sleep(HALF_SECOND);
        Assert.assertTrue(client.isInitializedExercised());
    }

    @org.junit.Test
    public void terminate() throws InterruptedException {
    }


//        client.expectedNotifications.put("initialized", new Object());
//        client.joinOnEmpty();
//        Thread.sleep(3000);

//        verify(client).
//        CompletableFuture<Capabilities> capainitialize(InitializeRequestArguments args) {

//        Map<String, Object> args = new HashMap<>();
//        server.launch(args);
//        Thread.sleep(300);

//        Source source = new Source();
//        source.setPath("/home/saxcell/dev/boris/testcases/helloworld/helloworld.cpp");
//        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
//        sourceBreakpoint.setLine(Long.valueOf(9));
//        SetBreakpointsArguments setBreakpointsArguments = new SetBreakpointsArguments();
//        setBreakpointsArguments.setSource(source);
//        setBreakpointsArguments.setBreakpoints(new SourceBreakpoint[] {sourceBreakpoint});
//        CompletableFuture<SetBreakpointsResponse> future = server.setBreakpoints(setBreakpointsArguments);
//        SetBreakpointsResponse setBreakpointsResponse = future.get();
//        Thread.sleep(3000);

}