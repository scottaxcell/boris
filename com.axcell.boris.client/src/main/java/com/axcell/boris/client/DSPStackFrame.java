package com.axcell.boris.client;

import com.axcell.boris.client.model.StackFrame;
import com.axcell.boris.client.model.Thread;
import com.axcell.boris.client.model.Variable;
import org.eclipse.lsp4j.debug.Scope;
import org.eclipse.lsp4j.debug.ScopesArguments;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class DSPStackFrame extends DSPDebugElement implements StackFrame {
    private DSPThread thread;
    private org.eclipse.lsp4j.debug.StackFrame stackFrame;
    private int depth;

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
        ScopesArguments scopesArguments = new ScopesArguments();
        scopesArguments.setFrameId(stackFrame.getId());
        Scope[] scopes = new Scope[0];
        try {
            scopes = getDebugClient().getDebugServer().scopes(scopesArguments).get().getScopes();
        }
        catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        List<DSPVariable> variables = new ArrayList<>();
        for (Scope scope : scopes) {
            DSPVariable variable = new DSPVariable(this, scope.getVariablesReference(), scope.getName(), "");
            variables.add(variable);
        }
        return variables.toArray(new Variable[variables.size()]);
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