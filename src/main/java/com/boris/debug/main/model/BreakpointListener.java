package com.boris.debug.main.model;

public interface BreakpointListener {
    void breakpointAdded(Breakpoint breakpoint);

    void breakpointChanged(Breakpoint breakpoint);

    void breakpointRemoved(Breakpoint breakpoint);
}
