package com.boris.debug.client;

import com.boris.debug.server.GdbDebugProtocolServer;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.junit.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class GdbDebugProtocolClientTest {
    private GdbDebugProtocolServer server;
    private Launcher<IDebugProtocolClient> serverLauncher;
    private Future<?> serverListening;
    private Launcher<IDebugProtocolServer> clientLauncher;
    private Future<?> clientListening;
    PipedInputStream inClient = new PipedInputStream();
    PipedOutputStream outClient = new PipedOutputStream();
    PipedInputStream inServer = new PipedInputStream();
    PipedOutputStream outServer = new PipedOutputStream();



    @org.junit.Before
    public void setUp() throws Exception {
        server = new GdbDebugProtocolServer();
        serverLauncher = DSPLauncher.createServerLauncher(server, inServer, outServer);
        serverListening = serverLauncher.startListening();
        inClient.connect(outServer);
        outClient.connect(inServer);
    }

    @org.junit.After
    public void tearDown() throws Exception {
    }

    @Test
    public void initialize() throws ExecutionException, InterruptedException, IOException {
        GdbDebugProtocolClient client = new GdbDebugProtocolClient(inClient, outClient);
        client.initialize();
    }
}
