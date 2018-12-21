package com.boris.debug.client;

import com.boris.debug.main.model.IStackFrame;
import com.boris.debug.main.model.IThread;
import org.eclipse.lsp4j.debug.StackFrame;
import org.eclipse.lsp4j.debug.StackTraceArguments;
import org.eclipse.lsp4j.debug.StackTraceResponse;
import org.eclipse.lsp4j.debug.Thread;

import java.util.ArrayList;
import java.util.List;

public class DSPThread extends DSPDebugElement implements IThread {
    Long id;
    String name;
    List<DSPStackFrame> stackFrames;

    public DSPThread(GdbDebugClient client, Thread thread) {
        super(client);
        this.name = thread.getName();
        this.id = thread.getId();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IStackFrame[] getStackFrames() {
        if (stackFrames == null)
            getStackFrames_();
        return stackFrames.toArray(new IStackFrame[stackFrames.size()]);
    }

    private void getStackFrames_() {
        if (stackFrames == null)
            stackFrames = new ArrayList<>();

        StackTraceArguments stackTraceArguments = new StackTraceArguments();
        stackTraceArguments.setThreadId(id);
        stackTraceArguments.setStartFrame(0L);
        stackTraceArguments.setLevels(20L);
        StackTraceResponse response = complete(getDebugClient().getDebugServer().stackTrace(stackTraceArguments));
        StackFrame[] newStackFrames = response.getStackFrames();
        for (int i = 0; i < newStackFrames.length; i++) {
            if (i < stackFrames.size())
                stackFrames.set(i, stackFrames.get(i).replace(newStackFrames[i], i));
            else
                stackFrames.add(new DSPStackFrame(this, newStackFrames[i], i));
        }
    }

    @Override
    public IStackFrame getTopStackFrame() {
        if (stackFrames == null)
            getStackFrames_();
        return stackFrames.get(0);
    }
}
