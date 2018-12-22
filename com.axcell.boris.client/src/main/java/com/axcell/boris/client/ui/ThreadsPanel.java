package com.axcell.boris.client.ui;

import com.axcell.boris.client.GdbDebugClient;
import com.axcell.boris.client.debug.dsp.DSPStackFrame;
import com.axcell.boris.client.debug.dsp.DSPThread;
import com.axcell.boris.client.debug.event.DebugEvent;
import com.axcell.boris.client.debug.event.DebugEventListener;
import com.axcell.boris.client.debug.model.StackFrame;
import com.axcell.boris.utils.Utils;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;

public class ThreadsPanel extends JPanel implements DebugEventListener {
    private GdbDebugClient client;
    private JTree tree;
    private ThreadTreeModel model;

    ThreadsPanel(GdbDebugClient client) {
        super(new BorderLayout());
        this.client = client;
        Boris.getDebugEventMgr().addListener(this);
        init();
    }

    private void init() {
        setBorder(BorderFactory.createTitledBorder("Threads"));
        model = new ThreadTreeModel(new ThreadTreeNode("Root Node - Hello World"));
        tree = new JTree(model);
        tree.setCellRenderer(new ThreadTreeCellRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        add(new JScrollPane(tree), BorderLayout.CENTER);
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
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    model.updateModel();
                    return true;
                }

                @Override
                protected void done() {
                    SwingUtilities.invokeLater(() -> {
                        StringBuilder sb = new StringBuilder("UPDATING threads panel..");
                        for (DSPThread t : model.threads) {
                            String s = String.format("Thread {[%s] %s}", t.getId(), t.getName());
                            sb.append(s);
                            for (StackFrame sf : t.getStackFrames()) {
                                s = String.format(" StackFrame {%s %s}", ((DSPStackFrame) sf).getDepth(), sf.getName());
                                sb.append(s);
                            }
                        }
                        Utils.debug("SGA -- " + sb.toString());

                        tree.setModel(model);
                        for (int i = 0; i < tree.getRowCount(); i++)
                            tree.expandRow(i);
                    });
                }
            };
            worker.execute();
        }
        else if (event.getType() == DebugEvent.CONTINUED) {
            // TODO
        }
    }

    private class ThreadTreeModel extends DefaultTreeModel {
        private DSPThread[] threads;

        ThreadTreeModel(ThreadTreeNode root) {
            super(root);
        }

        void updateModel() {
            if (client == null)
                return;

            int numChildren = root.getChildCount();
            for (int i = 0; i < numChildren; i++)
                removeNodeFromParent((MutableTreeNode) root.getChildAt(i));

            Utils.debug("ThreadsPanel: client.getThreads()");
            threads = client.getThreads();
            for (DSPThread thread : threads) {
                ThreadTreeNode threadNode = new ThreadTreeNode(thread);
                insertNodeInto(threadNode, (MutableTreeNode) root, root.getChildCount());
                StackFrame[] stackFrames = thread.getStackFrames();
                if (stackFrames.length != 0) {
                    for (StackFrame stackFrame : stackFrames) {
                        ThreadTreeNode stackNode = new ThreadTreeNode(stackFrame);
                        insertNodeInto(stackNode, threadNode, threadNode.getChildCount());
                    }
                }
            }
            reload();
        }
    }

    private class ThreadTreeNode extends DefaultMutableTreeNode {
        private Object object;

        ThreadTreeNode(Object object) {
            this.object = object;
        }

        Object getObject() {
            return object;
        }
    }

    private static class ThreadTreeCellRenderer implements TreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (value instanceof ThreadTreeNode) {
                Object object = ((ThreadTreeNode) value).getObject();
                if (object instanceof String) {
                    return new JLabel("Application " + String.valueOf(object));
                }
                else if (object instanceof DSPThread) {
                    DSPThread thread = (DSPThread) object;
                    String s = String.format("Thread [%s] %s", thread.getId(), thread.getName());
                    return new JLabel(s);
                }
                else if (object instanceof DSPStackFrame) {
                    DSPStackFrame stackFrame = (DSPStackFrame) object;
                    String s = String.format("%s %s", stackFrame.getDepth(), stackFrame.getName());
                    return new JLabel(s);
                }
            }
            return new JLabel("bad tree node");
        }
    }

}
