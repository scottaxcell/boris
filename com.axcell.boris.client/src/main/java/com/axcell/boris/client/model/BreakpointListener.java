package com.axcell.boris.client.model;

public interface BreakpointListener {
    void breakpointAdded(Breakpoint breakpoint);

    void breakpointChanged(Breakpoint breakpoint);

    void breakpointRemoved(Breakpoint breakpoint);
}
