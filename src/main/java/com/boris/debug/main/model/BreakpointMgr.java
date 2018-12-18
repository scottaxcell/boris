package com.boris.debug.main.model;

import java.util.ArrayList;
import java.util.List;

public class BreakpointMgr implements IBreakpointMgr {
    List<IBreakpoint> breakpoints;
    List<IBreakpointListener> listeners;

    public BreakpointMgr() {
        breakpoints = new ArrayList<>();
        listeners = new ArrayList<>();
    }

    @Override
    public void addBreakpoint(IBreakpoint breakpoint) {
        if (!breakpoints.contains(breakpoint)) {
            breakpoints.add(breakpoint);
            notifyListenersOfAdd(breakpoint);
        }
    }

    private void notifyListenersOfAdd(IBreakpoint breakpoint) {
        for (IBreakpointListener listener : listeners) {
            listener.breakpointAdded(breakpoint);
        }
    }

    @Override
    public void removeBreakpoint(IBreakpoint breakpoint) {
        breakpoints.remove(breakpoint);
        notifyListenersOfRemove(breakpoint);
    }

    private void notifyListenersOfRemove(IBreakpoint breakpoint) {
        for (IBreakpointListener listener : listeners) {
            listener.breakpointRemoved(breakpoint);
        }
    }

    @Override
    public void addBreakpointListener(IBreakpointListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    @Override
    public void removeBreakpointListener(IBreakpointListener listener) {
        listeners.remove(listener);
    }

    @Override
    public IBreakpoint[] getBreakpoints() {
        return breakpoints.toArray(new IBreakpoint[breakpoints.size()]);
    }

}
