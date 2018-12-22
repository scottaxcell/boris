package com.axcell.boris.client;

import com.axcell.boris.client.event.DebugEvent;
import com.axcell.boris.client.model.BreakpointMgr;
import com.axcell.boris.client.model.IBreakpointMgr;
import com.axcell.boris.client.ui.Boris;
import com.axcell.boris.dap.gdb.GdbDebugServer;
import com.axcell.boris.dap.gdb.Target;
import com.axcell.boris.utils.Utils;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.Launcher;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private ExecutorService threadPool;
    private Capabilities capabilities;
    /**
     * Global breakpoint manager
     */
    private BreakpointMgr breakpointMgr;
    private DSPBreakpointMgr dspBreakpointMgr;
    /**
     * The initialized event will mark this as complete
     */
    private CompletableFuture<Void> initialized = new CompletableFuture<>();
    /**
     * Synchronized cached set of current threads.
     */
    private Map<Long, DSPThread> threads = Collections.synchronizedMap(new TreeMap<>());
    /**
     * Update the threads list from the debug adapter
     */
    private AtomicBoolean refreshThreads = new AtomicBoolean(true);


    public GdbDebugClient(Target target, BreakpointMgr breakpointMgr) {
        this.target = target;
        this.breakpointMgr = breakpointMgr;
    }

    public void initialize(int bogus) {
        // TODO take progress monitor as an argument
        CompletableFuture<Void> future = initialize();
        try {
            future.get();
        }
        catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private CompletableFuture<Void> initialize() {
        Utils.debug(this.getClass().getSimpleName() + ": initialize");

        try {
            inClient.connect(outServer);
            outClient.connect(inServer);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        debugServer = new GdbDebugServer(target, this);
        serverLauncher = DSPLauncher.createServerLauncher(debugServer, inServer, outServer);
        serverListening = serverLauncher.startListening();

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
                }).thenCombineAsync(initialized, (v1, v2) -> {
                    return (Void) null;
                }).thenCompose((v) -> {
                    dspBreakpointMgr = new DSPBreakpointMgr(getBreakpointMgr(), getDebugServer());
                    return dspBreakpointMgr.initialize();
                }).thenCompose((v) -> {
                    return getDebugServer().launch(new HashMap<String, Object>());
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
        DebugEvent event = new DebugEvent(DebugEvent.CONTINUED, this, args);
        Boris.getDebugEventMgr().fireEvent(event);
    }

    public void continueAllThreads() {
        for (DSPThread thread : getThreads()) {
            ContinueArguments args = new ContinueArguments();
            args.setThreadId(thread.getId());
            try {
                getDebugServer().continue_(new ContinueArguments()).get();
            }
            catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stopped(StoppedEventArguments args) {
        Utils.debug(this.getClass().getSimpleName() + ": stopped");
        DebugEvent event = new DebugEvent(DebugEvent.STOPPED, this, args);
        Boris.getDebugEventMgr().fireEvent(event);
    }

    @Override
    public void output(OutputEventArguments args) {
        Utils.debug(this.getClass().getSimpleName() + ": output: " + args.getOutput());
        if ("console".equals(args.getCategory())) {
            DebugEvent event = new DebugEvent(DebugEvent.CONSOLE_OUTPUT, this, args.getOutput());
            Boris.getDebugEventMgr().fireEvent(event);
        }
        else if ("target".equals(args.getCategory())){
            DebugEvent event = new DebugEvent(DebugEvent.TARGET_OUTPUT, this, args.getOutput());
            Boris.getDebugEventMgr().fireEvent(event);
        }
    }

    public GdbDebugServer getDebugServer() {
        return debugServer;
    }

    public IBreakpointMgr getBreakpointMgr() {
        return breakpointMgr;
    }

    public DSPThread[] getThreads() {
        if (!refreshThreads.getAndSet(false)) {
            synchronized (threads) {
                Collection<DSPThread> values = threads.values();
                return values.toArray(new DSPThread[values.size()]);
            }
        }
        try {
            CompletableFuture<ThreadsResponse> responseFuture = getDebugServer().threads();
            CompletableFuture<DSPThread[]> future = responseFuture.thenApplyAsync(threadsResponse -> {
                synchronized (threads) {
                    Map<Long, DSPThread> oldThreads = new TreeMap<>(threads);
                    threads.clear();
                    for (Thread thread : threadsResponse.getThreads()) {
                        DSPThread dspThread = oldThreads.get(thread.getId());
                        if (dspThread == null) {
                            dspThread = new DSPThread(this, thread);
                            // TODO dspThread.update(thread);
                            threads.put(thread.getId(), dspThread);
                        }
                    }
                    Collection<DSPThread> values = threads.values();
                    return values.toArray(new DSPThread[values.size()]);
                }
            });
            return future.get();
        }
        catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new DSPThread[0];
        }
    }

    public boolean isInitialized() {
        return initialized.isDone();
    }

    public Capabilities getCapabilities() {
        return capabilities;
    }
}
