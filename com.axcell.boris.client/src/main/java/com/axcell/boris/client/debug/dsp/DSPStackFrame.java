package com.axcell.boris.client.debug.dsp;

import com.axcell.boris.client.debug.model.StackFrame;
import com.axcell.boris.client.debug.model.Thread;
import com.axcell.boris.client.debug.model.Variable;
import org.eclipse.lsp4j.debug.Scope;
import org.eclipse.lsp4j.debug.ScopesArguments;
import org.eclipse.lsp4j.debug.VariablesArguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class DSPStackFrame extends DSPDebugElement implements StackFrame {
    private DSPThread thread;
    private org.eclipse.lsp4j.debug.StackFrame stackFrame;
    private int depth;
    private List<DSPVariable> variables = Collections.synchronizedList(new ArrayList<>());
    private AtomicBoolean refreshVariables = new AtomicBoolean(true);

    public DSPStackFrame(DSPThread thread, org.eclipse.lsp4j.debug.StackFrame stackFrame, int depth) {
        super(thread.getDebugClient());
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
        if (!refreshVariables.getAndSet(false)) {
            synchronized (variables) {
                return variables.toArray(new DSPVariable[variables.size()]);
            }
        }

        variables.clear();

        try {
            ScopesArguments scopesArguments = new ScopesArguments();
            scopesArguments.setFrameId(stackFrame.getId());
            Scope[] scopes = getDebugClient().getDebugServer().scopes(scopesArguments).get().getScopes();

            // TODO support arguments scope, only supporting locals scope atm
            VariablesArguments variablesArguments = new VariablesArguments();
            variablesArguments.setVariablesReference(scopes[0].getVariablesReference());

            CompletableFuture<DSPVariable[]> future = getDebugClient().getDebugServer().variables(variablesArguments)
                    .thenApply(response -> {
                        synchronized (variables) {
                            org.eclipse.lsp4j.debug.Variable[] responseVariables = response.getVariables();
                            for (org.eclipse.lsp4j.debug.Variable responseVariable : responseVariables) {
                                DSPVariable variable = new DSPVariable(this, responseVariable);
                                variables.add(variable);
                            }
                            return this.variables.toArray(new DSPVariable[this.variables.size()]);
                        }
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
