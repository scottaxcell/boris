package com.boris.debug.main.model;

/**
 * Represents an execution context in a suspended thread.
 */
public interface IStackFrame {
    String getName();

    IVariable[] getVariables();

    IThread getThread();

    Long getLineNumber();
}
