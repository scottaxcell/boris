package com.boris.debug.main.model;

import java.util.ArrayList;
import java.util.List;

public class BreakpointMgr implements IBreakpointMgr {
    private static BreakpointMgr breakpointMgr;
    private List<Breakpoint> breakpoints;
    private List<BreakpointListener> listeners;

    private BreakpointMgr() {
        breakpoints = new ArrayList<>();
        listeners = new ArrayList<>();
    }

    public static BreakpointMgr getInstance() {
        if (breakpointMgr == null)
            breakpointMgr = new BreakpointMgr();
        return breakpointMgr;
    }

    @Override
    public void addBreakpoint(Breakpoint breakpoint) {
        if (!breakpoints.contains(breakpoint)) {
            breakpoints.add(breakpoint);
            notifyListenersOfAdd(breakpoint);
        }
    }

    private void notifyListenersOfAdd(Breakpoint breakpoint) {
        for (BreakpointListener listener : listeners) {
            listener.breakpointAdded(breakpoint);
        }
    }

    @Override
    public void removeBreakpoint(Breakpoint breakpoint) {
        breakpoints.remove(breakpoint);
        notifyListenersOfRemove(breakpoint);
    }

    private void notifyListenersOfRemove(Breakpoint breakpoint) {
        for (BreakpointListener listener : listeners) {
            listener.breakpointRemoved(breakpoint);
        }
    }

    @Override
    public void addBreakpointListener(BreakpointListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    @Override
    public void removeBreakpointListener(BreakpointListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void setBreakpointEnabled(Breakpoint breakpoint, boolean enabled) {
        if (breakpoints.contains(breakpoint)) {
            for (Breakpoint bp : breakpoints) {
                if (bp.equals(breakpoint)) {
                    bp.setEnabled(enabled);
                    notifyListenersOfChange(bp);
                    break;
                }
            }
        }
        else {
            addBreakpoint(breakpoint);
        }
    }

    private void notifyListenersOfChange(Breakpoint breakpoint) {
        for (BreakpointListener listener : listeners) {
            listener.breakpointRemoved(breakpoint);
        }
    }

    @Override
    public Breakpoint[] getBreakpoints() {
        return breakpoints.toArray(new Breakpoint[breakpoints.size()]);
    }

}
