package com.axcell.boris.dap.client.debug.event;

import java.util.ArrayList;
import java.util.List;

public class DebugEventMgr {
    private static DebugEventMgr debugEventMgr;

    private List<DebugEventListener> listeners;

    private DebugEventMgr() {
        listeners = new ArrayList<>();
    }

    public static DebugEventMgr getInstance() {
        if (debugEventMgr == null)
            debugEventMgr = new DebugEventMgr();
        return debugEventMgr;
    }

    public void addListener(DebugEventListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public void removeListener(DebugEventListener listener) {
        listeners.remove(listener);
    }

    public void fireEvent(final DebugEvent event) {
        for (DebugEventListener listener : listeners) {
            listener.handleEvent(event);
        }
    }
}
