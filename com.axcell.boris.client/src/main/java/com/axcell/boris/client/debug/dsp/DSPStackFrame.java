package com.axcell.boris.client.debug.dsp;

import com.axcell.boris.client.debug.model.StackFrame;
import com.axcell.boris.client.debug.model.Thread;
import com.axcell.boris.client.debug.model.Variable;
import org.eclipse.lsp4j.debug.Scope;
import org.eclipse.lsp4j.debug.ScopesArguments;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DSPStackFrame extends DSPDebugElement implements StackFrame {
    private DSPThread thread;
    private org.eclipse.lsp4j.debug.StackFrame stackFrame;
    private int depth;

    public DSPStackFrame(DSPThread thread, org.eclipse.lsp4j.debug.StackFrame stackFrame, int depth) {
        super(thread.getDebugTarget());
        this.thread = thread;
        this.stackFrame = stackFrame;
        this.depth = depth;
    }

    @Override
    public String getName() {
        return stackFrame.getName();
    }

    @Override
    public Variable[] getVariables() {
        try {
            ScopesArguments args = new ScopesArguments();
            args.setFrameId(stackFrame.getId());
            CompletableFuture<DSPVariable[]> future = getDebugTarget().getDebugServer().scopes(args)
                    .thenApply(response -> {
                        List<DSPVariable> variables = new ArrayList<>();
                        for (Scope scope : response.getScopes()) {
                            DSPVariable variable = new DSPVariable(this, scope.getVariablesReference(), scope.getName(), "");
                            variables.add(variable);
                        }
                        return variables.toArray(new DSPVariable[variables.size()]);
                    });
            return future.get();
        }
        catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new DSPVariable[0];
        }
    }

    @Override
    public Thread getThread() {
        return thread;
    }

    @Override
    public Long getLineNumber() {
        return stackFrame.getLine();
    }

    @Override
    public boolean hasVariables() {
        return true;
    }

    public DSPStackFrame replace(org.eclipse.lsp4j.debug.StackFrame stackFrame, int depth) {
        if (depth == this.depth && Objects.equals(stackFrame.getSource(), this.stackFrame.getSource())) {
            this.stackFrame = stackFrame;
            return this;
        }
        return new DSPStackFrame(thread, stackFrame, depth);
    }

    public int getDepth() {
        return depth;
    }
}
