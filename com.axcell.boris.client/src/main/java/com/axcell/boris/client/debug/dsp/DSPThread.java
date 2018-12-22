package com.axcell.boris.client.debug.dsp;

import com.axcell.boris.client.GdbDebugClient;
import com.axcell.boris.client.debug.model.StackFrame;
import com.axcell.boris.client.debug.model.Thread;
import org.eclipse.lsp4j.debug.StackTraceArguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class DSPThread extends DSPDebugElement implements Thread {
    private Long id;
    private String name;
    private List<DSPStackFrame> stackFrames = Collections.synchronizedList(new ArrayList<>());
    private AtomicBoolean refreshFrames = new AtomicBoolean(true);

    public DSPThread(GdbDebugClient client, org.eclipse.lsp4j.debug.Thread thread) {
        super(client);
        this.name = thread.getName();
        this.id = thread.getId();
    }

    @Override
    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
    }

    @Override
    public StackFrame[] getStackFrames() {
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
            CompletableFuture<DSPStackFrame[]> future = getDebugClient().getDebugServer().stackTrace(stackTraceArguments)
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
    public StackFrame getTopStackFrame() {
        StackFrame[] stackFrames = getStackFrames();
        if (stackFrames.length > 0)
            return stackFrames[0];
        else
            return null;
    }
}
