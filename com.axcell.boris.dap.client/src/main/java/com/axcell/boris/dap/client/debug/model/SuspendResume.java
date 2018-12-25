package com.axcell.boris.dap.client.debug.model;

public interface SuspendResume {
    boolean canResume();

    boolean canSuspend();

    boolean isSuspended();

    void resume();

    void suspend();
}
