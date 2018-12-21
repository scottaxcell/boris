package com.boris.debug.main.ui.event;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class MyEventMgr {
    private List<IMyEventListener> listeners;

    public MyEventMgr() {
        listeners = new ArrayList<>();
    }

    public void addListener(IMyEventListener listener) {
        if (listeners.contains(listener))
            listeners.add(listener);
    }

    public void removeListener(IMyEventListener listener) {
        listeners.remove(listener);
    }

    public void fireEvent(final MyEvent event) {
        if (SwingUtilities.isEventDispatchThread()) {
            fireEvent_(event);
        }
        else {
            SwingUtilities.invokeLater(() -> fireEvent_(event));
        }
    }

    private void fireEvent_(MyEvent event) {
        for (IMyEventListener listener : listeners) {
            listener.handleEvent(event);
        }
    }
}

