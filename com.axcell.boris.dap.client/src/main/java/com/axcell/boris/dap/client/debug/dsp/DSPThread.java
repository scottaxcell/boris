package com.axcell.boris.dap.client.debug.dsp;

import com.axcell.boris.dap.client.debug.model.Breakpoint;
import com.axcell.boris.dap.client.debug.model.Thread;
import org.eclipse.lsp4j.debug.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class DSPThread extends DSPDebugElement implements Thread {
    private final List<DSPStackFrame> stackFrames = Collections.synchronizedList(new ArrayList<>());
    private Long id;
    private String name;
    private AtomicBoolean refreshFrames = new AtomicBoolean(true);
    private boolean isSuspended;
    private boolean isStepping;

    public DSPThread(GDBDebugTarget debugTarget, org.eclipse.lsp4j.debug.Thread thread) {
        super(debugTarget);
        this.name = thread.getName();
        this.id = thread.getId();
    }

    @Override
    public Breakpoint[] getBreakpoints() {
        // TODO
        return new Breakpoint[0];
    }

    @Override
    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
    }

    @Override
    public DSPStackFrame[] getStackFrames() {
        if (!refreshFrames.getAndSet(false)) {
            synchronized (stackFrames) {
                return stackFrames.toArray(new DSPStackFrame[stackFrames.size()]);
            }
        }
        try {
            StackTraceArguments stackTraceArguments = new StackTraceArguments();
            stackTraceArguments.setThreadId(id);
            stackTraceArguments.setStartFrame(0L);
            stackTraceArguments.setLevels(20L);
            CompletableFuture<DSPStackFrame[]> future = getDebugTarget().getDebugServer().stackTrace(stackTraceArguments)
                    .thenApply(response -> {
                        synchronized (stackFrames) {
                            org.eclipse.lsp4j.debug.StackFrame[] newStackFrames = response.getStackFrames();
                            for (int i = 0; i < newStackFrames.length; i++) {
                                if (i < stackFrames.size())
                                    stackFrames.set(i, stackFrames.get(i).replace(newStackFrames[i], i));
                                else
                                    stackFrames.add(new DSPStackFrame(this, newStackFrames[i], i));
                            }
                            stackFrames.subList(newStackFrames.length, stackFrames.size()).clear();
                            return stackFrames.toArray(new DSPStackFrame[stackFrames.size()]);
                        }
                    });
            return future.get();
        }
        catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new DSPStackFrame[0];
        }
    }

    @Override
    public DSPStackFrame getTopStackFrame() {
        DSPStackFrame[] stackFrames = getStackFrames();
        if (stackFrames.length > 0)
            return stackFrames[0];
        else
            return null;
    }

    @Override
    public boolean hasStackFrames() {
        return false;
    }

    public void update(org.eclipse.lsp4j.debug.Thread thread) {
        if (!Objects.equals(getId(), thread.getId()))
            throw new IllegalStateException("attempting to update thread with different thread");
        name = thread.getName();
        refreshFrames.set(true);
    }

    @Override
    public boolean canResume() {
        return isSuspended();
    }

    @Override
    public boolean canSuspend() {
        return !isSuspended();
    }

    @Override
    public boolean isSuspended() {
        return isSuspended;
    }

    @Override
    public void resume() {
        isStepping = true;
        ContinueArguments args = new ContinueArguments();
        args.setThreadId(id);
        try {
            getDebugTarget().getDebugServer().continue_(args).get();
        }
        catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void suspend() {
        PauseArguments args = new PauseArguments();
        args.setThreadId(id);
        CompletableFuture<Void> future = getDebugTarget().getDebugServer().pause(args).thenAccept(response -> {
            isSuspended = true;
        });
        try {
            future.get();
        }
        catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean canStepInto() {
        return isSuspended();
    }

    @Override
    public boolean canStepOver() {
        return isSuspended();
    }

    @Override
    public boolean canStepReturn() {
        return isSuspended();
    }

    @Override
    public boolean isStepping() {
        return isStepping;
    }

    @Override
    public void stepInto() {
        isStepping = true;
        StepInArguments args = new StepInArguments();
        args.setThreadId(id);
        try {
            getDebugTarget().getDebugServer().stepIn(args).get();
        }
        catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stepOver() {
        isStepping = true;
        NextArguments args = new NextArguments();
        args.setThreadId(id);
        try {
            getDebugTarget().getDebugServer().next(args).get();
        }
        catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stepReturn() {
        isStepping = true;
        StepOutArguments args = new StepOutArguments();
        args.setThreadId(id);
        try {
            getDebugTarget().getDebugServer().stepOut(args).get();
        }
        catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
