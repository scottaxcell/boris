package com.axcell.boris.client.debug.model;

/**
* Represents a thread in a process.
 */
public interface Thread {
    String getName();

    StackFrame[] getStackFrames();

    StackFrame getTopStackFrame();
}
