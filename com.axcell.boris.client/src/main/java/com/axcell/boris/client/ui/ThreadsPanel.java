package com.axcell.boris.client.ui;

import com.axcell.boris.client.debug.dsp.DSPStackFrame;
import com.axcell.boris.client.debug.dsp.DSPThread;
import com.axcell.boris.client.debug.dsp.GDBDebugTarget;
import com.axcell.boris.client.debug.event.DebugEvent;
import com.axcell.boris.client.debug.event.DebugEventListener;
import com.axcell.boris.client.ui.event.GUIEvent;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.util.Optional;
import java.util.stream.Stream;

public class ThreadsPanel extends JPanel implements DebugEventListener {
    private GDBDebugTarget debugTarget;
    private JTree tree;
    private ThreadTreeModel model;
    private String targetName;

    ThreadsPanel() {
        super(new BorderLayout());
        Boris.getDebugEventMgr().addListener(this);
        init();
    }

    private void init() {
        setBorder(BorderFactory.createTitledBorder("Threads"));
        targetName = getDebugTarget() != null ? getDebugTarget().getName() : "Debug Adapter Target";
        model = new ThreadTreeModel(new ThreadTreeNode(targetName));
        tree = new JTree(model);
        tree.setCellRenderer(new ThreadTreeCellRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(new ThreadTreeListener());
        add(new JScrollPane(tree), BorderLayout.CENTER);
    }

    public void cleanup() {
        Boris.getDebugEventMgr().removeListener(this);
    }

    public GDBDebugTarget getDebugTarget() {
        return debugTarget;
    }

    public void setDebugTarget(GDBDebugTarget debugTarget) {
        this.debugTarget = debugTarget;
    }

    public void expandTree() {
        for (int i = 0; i < tree.getRowCount(); i++)
            tree.expandRow(i);
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
                        model.refreshTree();
                        expandTree();
                    });
                }
            };
            worker.execute();
        }
        else if (event.getType() == DebugEvent.CONTINUED) {
            // TODO
        }
        else if (event.getType() == DebugEvent.EXITED || event.getType() == DebugEvent.TERMINATED) {
            model.updateModel();
            SwingUtilities.invokeLater(() -> {
                model.refreshTree();
                expandTree();
            });
        }
    }

    public Optional<DSPThread> getSelectedThread() {
        Object leaf = tree.getLastSelectedPathComponent();
        if (leaf instanceof ThreadTreeNode) {
            Object object = ((ThreadTreeNode) leaf).getObject();
            if (object instanceof DSPThread)
                return Optional.ofNullable((DSPThread) object);
            else if (object instanceof DSPStackFrame) {
                TreeNode parent = ((ThreadTreeNode) leaf).getParent();
                if (parent instanceof ThreadTreeNode) {
                    object = ((ThreadTreeNode) parent).getObject();
                    if (object instanceof DSPThread)
                        return Optional.ofNullable((DSPThread) object);
                }
            }
        }
        return Optional.empty();
    }

    private static class ThreadTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            if (value instanceof ThreadTreeNode) {
                JLabel label = new JLabel();
                Object object = ((ThreadTreeNode) value).getObject();
                if (object instanceof String) {
                    this.setText(String.valueOf(object));
                }
                else if (object instanceof DSPThread) {
                    DSPThread thread = (DSPThread) object;
                    String s = String.format("Thread [%s] %s", thread.getId(), thread.getName());
                    this.setText(s);
                }
                else if (object instanceof DSPStackFrame) {
                    DSPStackFrame stackFrame = (DSPStackFrame) object;
                    String s = String.format("%s %s", stackFrame.getDepth(), stackFrame.getName());
                    this.setText(s);
                }
            }
            return this;
        }
    }

    private class ThreadTreeModel extends DefaultTreeModel {
        private DSPThread[] threads;

        ThreadTreeModel(ThreadTreeNode root) {
            super(root);
        }

        void updateModel() {
            if (debugTarget != null)
                threads = debugTarget.getThreads();
        }

        void refreshTree() {
            setRoot(new ThreadTreeNode(targetName));
            Stream.of(threads)
                    .map(this::createAndAddThread)
                    .forEach(this::createAndAddStackFrames);
            reload();
        }

        ThreadTreeNode createAndAddThread(DSPThread thread) {
            ThreadTreeNode threadTreeNode = new ThreadTreeNode(thread);
            ((ThreadTreeNode) root).add(threadTreeNode);
            return threadTreeNode;
        }

        void createAndAddStackFrames(ThreadTreeNode threadTreeNode) {
            Stream.of(((DSPThread) threadTreeNode.getObject()).getStackFrames())
                    .map(ThreadTreeNode::new)
                    .forEach(threadTreeNode::add);
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

    private class ThreadTreeListener implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            Object leaf = tree.getLastSelectedPathComponent();
            if (leaf instanceof ThreadTreeNode) {
                Object object = ((ThreadTreeNode) leaf).getObject();
                if (object instanceof DSPThread)
                    Boris.getGuiEventMgr().fireEvent(new GUIEvent(GUIEvent.THREAD_SELECTED, ThreadsPanel.this, object));
                else if (object instanceof DSPStackFrame)
                    Boris.getGuiEventMgr().fireEvent(new GUIEvent(GUIEvent.STACK_FRAME_SELECTED, ThreadsPanel.this, object));
            }
        }
    }
}
