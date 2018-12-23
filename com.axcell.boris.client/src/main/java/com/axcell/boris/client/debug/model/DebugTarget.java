package com.axcell.boris.client.debug.model;

import com.axcell.boris.dap.gdb.GDBDebugServer;

public interface DebugTarget extends SuspendResume {
    String getName();

    Process getProcess();

    Thread[] getThreads();

    boolean hasThreads();

    GDBDebugServer getDebugServer();
}
