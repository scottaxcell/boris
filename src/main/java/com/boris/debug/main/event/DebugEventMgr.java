package com.boris.debug.main.event;

import java.util.ArrayList;
import java.util.List;

public class DebugEventMgr {
    private List<DebugEventListener> listeners;

    public DebugEventMgr() {
        listeners = new ArrayList<>();
    }

    public void addListener(DebugEventListener listener) {
        if (listeners.contains(listener))
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
