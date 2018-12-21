package com.boris.debug.main.model;

/**
 * Represents an execution context in a suspended thread.
 */
public interface StackFrame {
    String getName();

    Variable[] getVariables();

    Thread getThread();

    Long getLineNumber();
}
