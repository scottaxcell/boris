package com.boris.debug.main.model;

/**
* Represents a thread in a process.
 */
public interface IThread {
    String getName();

    IStackFrame[] getStackFrames();

    IStackFrame getTopStackFrame();
}
