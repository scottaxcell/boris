package com.axcell.boris.dap.client.debug.model;

public interface BreakpointListener {
    void breakpointAdded(Breakpoint breakpoint);

    void breakpointChanged(Breakpoint breakpoint);

    void breakpointRemoved(Breakpoint breakpoint);
}
