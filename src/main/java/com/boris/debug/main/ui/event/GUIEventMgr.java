package com.boris.debug.main.ui.event;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class GUIEventMgr {
    private List<GUIEventListener> listeners;

    public GUIEventMgr() {
        listeners = new ArrayList<>();
    }

    public void addListener(GUIEventListener listener) {
        if (listeners.contains(listener))
            listeners.add(listener);
    }

    public void removeListener(GUIEventListener listener) {
        listeners.remove(listener);
    }

    public void fireEvent(final GUIEvent event) {
        if (SwingUtilities.isEventDispatchThread()) {
            fireEvent_(event);
        }
        else {
            SwingUtilities.invokeLater(() -> fireEvent_(event));
        }
    }

    private void fireEvent_(GUIEvent event) {
        for (GUIEventListener listener : listeners) {
            listener.handleEvent(event);
        }
    }
}

