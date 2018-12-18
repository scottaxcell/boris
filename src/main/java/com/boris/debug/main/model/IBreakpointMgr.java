package com.boris.debug.main.model;

public interface IBreakpointMgr {
    void addBreakpoint(IBreakpoint breakpoint);

    void removeBreakpoint(IBreakpoint breakpoint);

    void addBreakpointListener(IBreakpointListener listener);

    void removeBreakpointListener(IBreakpointListener listener);

    IBreakpoint[] getBreakpoints();
}
