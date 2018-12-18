package com.boris.debug.client;

import com.boris.debug.main.model.BreakpointMgr;
import com.boris.debug.main.model.IBreakpointMgr;
import com.boris.debug.server.GdbDebugServer;
import com.boris.debug.server.Target;
import com.boris.debug.utils.Utils;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.Launcher;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class GdbDebugClient implements IDebugProtocolClient {
    /**
     * lsp4e DSPDebugTarget for reference:
     * https://github.com/vladdu/lsp4e/blob/6f48292b40bb66593790e8c11415722cdeb9c4e3/org.eclipse.lsp4e.debug/src/org/eclipse/lsp4e/debug/debugmodel/DSPDebugTarget.java
     */

    private Target target;
    private GdbDebugServer debugServer;
    private Launcher<IDebugProtocolClient> serverLauncher;
    private Future<?> serverListening;
    private Launcher<IDebugProtocolServer> clientLauncher;
    private Future<?> clientListening;
    private PipedInputStream inClient = new PipedInputStream();
    private PipedOutputStream outClient = new PipedOutputStream();
    private PipedInputStream inServer = new PipedInputStream();
    private PipedOutputStream outServer = new PipedOutputStream();

    private CompletableFuture<Void> initialized = new CompletableFuture<>();
    private ExecutorService threadPool;
    private Capabilities capabilities;
    private BreakpointMgr breakpointMgr;
    private DSPBreakpointMgr dspBreakpointMgr;

    public GdbDebugClient(Target target, BreakpointMgr breakpointMgr) {
        this.target = target;
        this.breakpointMgr = breakpointMgr;
    }

    public CompletableFuture<Void> initialize() {
        Utils.debug(this.getClass().getSimpleName() + ": initialize");


        try {
            inClient.connect(outServer);
            outClient.connect(inServer);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        debugServer = new GdbDebugServer(target);
        serverLauncher = DSPLauncher.createServerLauncher(debugServer, inServer, outServer);
        serverListening = serverLauncher.startListening();

        debugServer.setRemoteProxy(this);
        clientLauncher = DSPLauncher.createClientLauncher(this, inClient, outClient);
        clientListening = clientLauncher.startListening();

        InitializeRequestArguments initializeRequestArguments = new InitializeRequestArguments();
        initializeRequestArguments.setClientID("com.boris.debug");
        initializeRequestArguments.setAdapterID("adapterId");
        initializeRequestArguments.setPathFormat("path");
        initializeRequestArguments.setSupportsVariableType(true);
        initializeRequestArguments.setSupportsVariablePaging(true);
        initializeRequestArguments.setLinesStartAt1(true);
        initializeRequestArguments.setColumnsStartAt1(true);
        initializeRequestArguments.setSupportsRunInTerminalRequest(true);

        CompletableFuture<Void> future = getDebugServer().initialize(initializeRequestArguments)
                .thenAccept((Capabilities capabilities) -> {
                    this.capabilities = capabilities;
                }).thenCompose((v) -> {
                    return getDebugServer().launch(new HashMap<String, Object>());
                }).thenCombineAsync(initialized, (v1, v2) -> {
                    return (Void) null;
                }).thenCompose((v) -> {
                    dspBreakpointMgr = new DSPBreakpointMgr(getBreakpointMgr(), getDebugServer());
                    return dspBreakpointMgr.initialize();
                }).thenCompose((v) -> {
                    return getDebugServer().configurationDone(new ConfigurationDoneArguments());
                });
        return future;
    }

    @Override
    public void initialized() {
        Utils.debug(this.getClass().getSimpleName() + ": initialized");
        initialized.complete(null);
    }

    private void sendSetBreakpointsRequest() {
        // TODO
        SetBreakpointsArguments setBreakpointsArguments = new SetBreakpointsArguments();
        getDebugServer().setBreakpoints(setBreakpointsArguments);
    }

    private void sendConfigurationDoneRequest() {
        ConfigurationDoneArguments configurationDoneArguments = new ConfigurationDoneArguments();
        getDebugServer().configurationDone(configurationDoneArguments);
    }

    @Override
    public void exited(ExitedEventArguments args) {
        Utils.debug(this.getClass().getSimpleName() + ": exited with code = " + args.getExitCode());
    }

    @Override
    public void terminated(TerminatedEventArguments args) {
        Utils.debug(this.getClass().getSimpleName() + ": terminated");
    }

    @Override
    public void continued(ContinuedEventArguments args) {
        Utils.debug(this.getClass().getSimpleName() + ": continued");
    }

    @Override
    public void stopped(StoppedEventArguments args) {
        Utils.debug(this.getClass().getSimpleName() + ": stopped");
    }

    public GdbDebugServer getDebugServer() {
        return debugServer;
    }

    public IBreakpointMgr getBreakpointMgr() {
        return breakpointMgr;
    }
}
