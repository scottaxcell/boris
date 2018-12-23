package com.axcell.boris.client.debug.model;

public interface SuspendResume {
    boolean canResume();

    boolean canSuspend();

    boolean isSuspended();

    void resume();

    void suspend();
}
