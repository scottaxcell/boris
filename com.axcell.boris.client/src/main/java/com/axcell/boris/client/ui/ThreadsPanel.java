package com.axcell.boris.client.ui;

import com.axcell.boris.client.DSPThread;
import com.axcell.boris.client.GdbDebugClient;
import com.axcell.boris.client.event.DebugEvent;
import com.axcell.boris.client.event.DebugEventListener;
import com.axcell.boris.utils.Utils;

import javax.swing.*;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;

public class ThreadsPanel extends JPanel implements DebugEventListener {
    private GdbDebugClient client;

    public ThreadsPanel() {
        super(new BorderLayout());
        Boris.getDebugEventMgr().addListener(this);
        init();
    }

    private void init() {
    }

    public void cleanup() {
        Boris.getDebugEventMgr().removeListener(this);
    }

    public GdbDebugClient getClient() {
        return client;
    }

    public void setClient(GdbDebugClient client) {
        this.client = client;
    }

    @Override
    public void handleEvent(DebugEvent event) {
        if (event.getType() == DebugEvent.STOPPED) {
            // TODO on debuggee stopped event update model
            DSPThread[] threads = client.getThreads();
            Utils.debug("stopped event in threadspanel");
        }
        else if (event.getType() == DebugEvent.CONTINUED) {
            // TODO
        }
    }

    private static class ThreadTreeModel implements TreeModel {
        private List<DSPThread> threads;

        @Override
        public Object getRoot() {
            return null;
        }

        @Override
        public Object getChild(Object parent, int index) {
            return null;
        }

        @Override
        public int getChildCount(Object parent) {
            return 0;
        }

        @Override
        public boolean isLeaf(Object node) {
            return false;
        }

        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {

        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            return 0;
        }

        @Override
        public void addTreeModelListener(TreeModelListener l) {

        }

        @Override
        public void removeTreeModelListener(TreeModelListener l) {

        }
        // TODO only provide stacks if they exist - that'll be at suspension time
        /**
         * app name
         *    thread0
         *       stack0
         *       ...
         *       stackN
         *    ...
         *    threadN
         *       stack0
         *       ...
         *       stackN
         */
    }
}
