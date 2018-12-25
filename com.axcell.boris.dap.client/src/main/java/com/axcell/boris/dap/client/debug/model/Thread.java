package com.axcell.boris.dap.client.debug.model;

/**
 * Represents a thread in a process.
 */
public interface Thread extends SuspendResume, Step {
    Breakpoint[] getBreakpoints();

    String getName();

    StackFrame[] getStackFrames();

    StackFrame getTopStackFrame();

    boolean hasStackFrames();
}
