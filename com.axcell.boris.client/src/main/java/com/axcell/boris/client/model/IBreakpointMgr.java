package com.axcell.boris.client.model;

public interface IBreakpointMgr {
    void addBreakpoint(Breakpoint breakpoint);

    void removeBreakpoint(Breakpoint breakpoint);

    void addBreakpointListener(BreakpointListener listener);

    void removeBreakpointListener(BreakpointListener listener);

    void setBreakpointEnabled(Breakpoint breakpoint, boolean enabled);

    Breakpoint[] getBreakpoints();
}
