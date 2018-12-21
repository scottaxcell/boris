package com.boris.debug.main.ui;

import com.boris.debug.client.DSPThread;
import com.boris.debug.client.GdbDebugClient;
import com.boris.debug.main.ui.event.IMyEventListener;
import com.boris.debug.main.ui.event.MyEvent;

import javax.swing.*;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;

public class ThreadsPanel extends JPanel implements IMyEventListener {
    GdbDebugClient client;

    public ThreadsPanel() {
        super(new BorderLayout());
        init();
    }

    private void init() {
    }

    public GdbDebugClient getClient() {
        return client;
    }

    public void setClient(GdbDebugClient client) {
        this.client = client;
    }

    @Override
    public void handleEvent(MyEvent event) {
        // TODO on debuggee stopped event update model
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
