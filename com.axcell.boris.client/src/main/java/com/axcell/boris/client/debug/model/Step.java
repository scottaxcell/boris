package com.axcell.boris.client.debug.model;

public interface Step {
    boolean canStepInto();

    boolean canStepOver();

    boolean canStepReturn();

    boolean isStepping();

    void stepInto();

    void stepOver();

    void stepReturn();
}
