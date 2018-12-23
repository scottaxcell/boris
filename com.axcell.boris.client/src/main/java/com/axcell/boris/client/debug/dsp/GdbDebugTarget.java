package com.axcell.boris.client.debug.dsp;

import com.axcell.boris.client.debug.event.DebugEvent;
import com.axcell.boris.client.debug.model.BreakpointMgr;
import com.axcell.boris.client.debug.model.DebugTarget;
import com.axcell.boris.client.debug.model.GlobalBreakpointMgr;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class GdbDebugTarget extends DSPDebugElement implements DebugTarget, IDebugProtocolClient {
    /**
     * Synchronized cached set of current threads.
     */
    private final Map<Long, DSPThread> threads = Collections.synchronizedMap(new TreeMap<>());
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
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private Capabilities capabilities;
    /**
     * Global breakpoint manager
     */
    private GlobalBreakpointMgr globalBreakpointMgr;
    private DSPBreakpointMgr dspBreakpointMgr;
    /**
     * The initialized event will mark this as complete
     */
    private CompletableFuture<Void> initialized = new CompletableFuture<>();
    /**
     * Update the threads list from the dsp adapter
     */
    private AtomicBoolean refreshThreads = new AtomicBoolean(true);
    private boolean isTerminated;


    public GdbDebugTarget(Target target, GlobalBreakpointMgr globalBreakpointMgr) {
        super(null);
        this.target = target;
        this.globalBreakpointMgr = globalBreakpointMgr;
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
        initializeRequestArguments.setClientID("com.boris.dsp");
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
                    dspBreakpointMgr = new DSPBreakpointMgr(getGlobalBreakpointMgr(), getDebugServer());
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
        DebugEvent event = new DebugEvent(DebugEvent.EXITED, this, args);
        Boris.getDebugEventMgr().fireEvent(event);
    }

    @Override
    public void terminated(TerminatedEventArguments args) {
        terminated();
        DebugEvent event = new DebugEvent(DebugEvent.TERMINATED, this, args);
        Boris.getDebugEventMgr().fireEvent(event);
    }

    private void terminated() {
        isTerminated = true;
        clientListening.cancel(true);
        if (dspBreakpointMgr != null)
            dspBreakpointMgr.cleanup();
    }

    @Override
    public void continued(ContinuedEventArguments args) {
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

    public void stepOver(Long threadId) {
        NextArguments args = new NextArguments();
        if (threadId != null)
            args.setThreadId(threadId);
        try {
            getDebugServer().next(args).get();
        }
        catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopped(StoppedEventArguments args) {
        refreshThreads.set(true);
        if (args.getAllThreadsStopped()) {
            for (DSPThread thread : getThreads()) {
                thread.suspend();
            }
        }
        else {
            DSPThread thread = getThread(args.getThreadId());
            if (thread != null)
                thread.suspend();
        }
        DebugEvent event = new DebugEvent(DebugEvent.STOPPED, this, args);
        Boris.getDebugEventMgr().fireEvent(event);
    }

    public DSPThread getThread(Long threadId) {
        if (threadId == null)
            return null;
        for (DSPThread thread : getThreads()) {
            if (thread.getId() == threadId)
                return thread;
        }
        return null;
    }

    @Override
    public void output(OutputEventArguments args) {
        if ("console".equals(args.getCategory())) {
            DebugEvent event = new DebugEvent(DebugEvent.CONSOLE_OUTPUT, this, args.getOutput());
            Boris.getDebugEventMgr().fireEvent(event);
        }
        else if ("target".equals(args.getCategory())) {
            DebugEvent event = new DebugEvent(DebugEvent.TARGET_OUTPUT, this, args.getOutput());
            Boris.getDebugEventMgr().fireEvent(event);
        }
    }

    public GdbDebugServer getDebugServer() {
        return debugServer;
    }

    public BreakpointMgr getGlobalBreakpointMgr() {
        return globalBreakpointMgr;
    }

    @Override
    public String getName() {
        // TODO
        return null;
    }

    @Override
    public Process getProcess() {
        // TODO
        return null;
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
                        if (dspThread == null)
                            dspThread = new DSPThread(this, thread);
                        dspThread.update(thread);
                        threads.put(thread.getId(), dspThread);
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

    @Override
    public boolean hasThreads() {
        return getThreads().length > 0;
    }

    public DSPStackFrame[] getStackFrames() {
        List<DSPStackFrame> stackFrames = new ArrayList<>();
        DSPThread[] threads = getThreads();
        for (DSPThread thread : threads)
            stackFrames.addAll(Arrays.asList(getStackFrames(thread)));
        return stackFrames.toArray(new DSPStackFrame[stackFrames.size()]);
    }

    public DSPStackFrame[] getStackFrames(DSPThread thread) {
        return (DSPStackFrame[]) thread.getStackFrames();
    }

    public DSPVariable[] getVariables() {
        List<DSPVariable> variables = new ArrayList<>();
        DSPStackFrame[] stackFrames = getStackFrames();
        for (DSPStackFrame stackFrame : stackFrames)
            variables.addAll(Arrays.asList(getVariables(stackFrame)));
        return variables.toArray(new DSPVariable[variables.size()]);
    }

    public DSPVariable[] getVariables(DSPStackFrame stackFrame) {
        return (DSPVariable[]) stackFrame.getVariables();
    }

    public boolean isInitialized() {
        return initialized.isDone();
    }

    public Capabilities getCapabilities() {
        return capabilities;
    }

    @Override
    public GdbDebugTarget getDebugTarget() {
        return this;
    }

    @Override
    public boolean canResume() {
        return !isTerminated() && isSuspended() && getThreads().length > 0;
    }

    private boolean isTerminated() {
        return isTerminated;
    }

    @Override
    public boolean canSuspend() {
        return !isTerminated() && !isSuspended() && getThreads().length > 0;
    }

    @Override
    public boolean isSuspended() {
        DSPThread[] dspThreads = getThreads();
        return Stream.of(dspThreads)
                .anyMatch(DSPThread::isSuspended);
    }

    @Override
    public void resume() {
        for (DSPThread thread : getThreads()) {
            thread.resume();
        }
    }

    @Override
    public void suspend() {
        for (DSPThread thread : getThreads()) {
            thread.suspend();
        }
    }
}
