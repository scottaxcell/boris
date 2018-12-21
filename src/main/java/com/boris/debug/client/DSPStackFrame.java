package com.boris.debug.client;

import com.boris.debug.main.model.IStackFrame;
import com.boris.debug.main.model.IThread;
import com.boris.debug.main.model.IVariable;
import org.eclipse.lsp4j.debug.Scope;
import org.eclipse.lsp4j.debug.ScopesArguments;
import org.eclipse.lsp4j.debug.StackFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DSPStackFrame extends DSPDebugElement implements IStackFrame {
    private DSPThread thread;
    private StackFrame stackFrame;
    private int depth;

    public DSPStackFrame(DSPThread thread, StackFrame stackFrame, int depth) {
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
    public IVariable[] getVariables() {
        ScopesArguments scopesArguments = new ScopesArguments();
        scopesArguments.setFrameId(stackFrame.getId());
        Scope[] scopes = complete(getDebugClient().getDebugServer().scopes(scopesArguments)).getScopes();
        List<DSPVariable> variables = new ArrayList<>();
        for (Scope scope : scopes) {
            DSPVariable variable = new DSPVariable(this, scope.getVariablesReference(), scope.getName(), "");
            variables.add(variable);
        }
        return variables.toArray(new IVariable[variables.size()]);
    }

    @Override
    public IThread getThread() {
        return thread;
    }

    @Override
    public Long getLineNumber() {
        return stackFrame.getLine();
    }

    public DSPStackFrame replace(StackFrame stackFrame, int depth) {
        if (depth == this.depth && Objects.equals(stackFrame.getSource(), this.stackFrame.getSource())) {
            this.stackFrame = stackFrame;
            return this;
        }
        return new DSPStackFrame(thread, stackFrame, depth);
    }
}
