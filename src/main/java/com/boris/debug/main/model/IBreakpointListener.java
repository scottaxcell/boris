package com.boris.debug.main.model;

public interface IBreakpointListener {
    void breakpointAdded(IBreakpoint breakpoint);

    void breakpointChanged(IBreakpoint breakpoint);

    void breakpointRemoved(IBreakpoint breakpoint);
}
