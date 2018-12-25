package com.axcell.boris.client.ui;

import com.axcell.boris.client.debug.dsp.DSPStackFrame;
import com.axcell.boris.client.debug.dsp.DSPThread;
import com.axcell.boris.client.debug.dsp.GDBDebugTarget;
import com.axcell.boris.client.debug.event.DebugEvent;
import com.axcell.boris.client.debug.event.DebugEventListener;
import com.axcell.boris.client.ui.event.GUIEvent;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.StoppedEventArguments;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
                        selectTopStackFrame(((StoppedEventArguments) event.getObject()).getThreadId());
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
            });
        }
    }

    private void selectTopStackFrame(Long threadId) {
        Stream.of(model.getThreads())
                .filter(thread -> thread.getId() == threadId)
                .findFirst()
                .ifPresent(thread -> selectTopStackFrame(thread));
    }

    private void selectTopStackFrame(DSPThread thread) {
        if (tree != null) {
            ThreadTreeNode threadNode = new ThreadTreeNode(thread);
            if (model.getTreeNodes().contains(threadNode)) {
                for (ThreadTreeNode node : model.getTreeNodes()) {
                    if (node.getObject() instanceof DSPThread) {
                        if (((DSPThread) node.getObject()).getId() == thread.getId()) {
                            if (node.getChildCount() > 0) {
                                TreeNode childAt = node.getChildAt(0);
                                TreeNode[] pathToRoot = model.getPathToRoot(childAt);
                                TreePath treePath = new TreePath(pathToRoot);
                                tree.setSelectionPath(treePath);
                                tree.scrollPathToVisible(treePath);
                            }
                        }
                    }
                }
            }
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
                    this.setText(getStackFrameText((DSPStackFrame) object));
                }
            }
            return this;
        }

        private String getStackFrameText(DSPStackFrame stackFrame) {
            StringBuilder sb = new StringBuilder();
            sb.append(stackFrame.getDepth() + " " + stackFrame.getName());

            if (stackFrame.getSource() == null)
                return sb.toString();

            if (stackFrame.getSource().getName() != null && stackFrame.getLineNumber() != null)
                sb.append(" at " + stackFrame.getSource().getName() + ":" + stackFrame.getLineNumber());

            return sb.toString();
        }
    }

    private class ThreadTreeModel extends DefaultTreeModel {
        private DSPThread[] threads;
        private Set<ThreadTreeNode> treeNodes = new LinkedHashSet<>();

        ThreadTreeModel(ThreadTreeNode root) {
            super(root);
        }

        void updateModel() {
            if (debugTarget != null)
                threads = debugTarget.getThreads();
        }

        void refreshTree() {
            treeNodes.clear();

            if (threads.length == 0) {
                setRoot(null);
                reload();
                return;
            }

            ThreadTreeNode root = new ThreadTreeNode(targetName);
            setRoot(root);
            treeNodes.add(root);

            Stream.of(threads)
                    .map(this::createAndAddThread)
                    .forEach(this::createAndAddStackFrames);

            reload();
        }

        ThreadTreeNode createAndAddThread(DSPThread thread) {
            ThreadTreeNode threadTreeNode = new ThreadTreeNode(thread);
            treeNodes.add(threadTreeNode);
            ((ThreadTreeNode) root).add(threadTreeNode);
            return threadTreeNode;
        }

        void createAndAddStackFrames(ThreadTreeNode threadTreeNode) {
            for (DSPStackFrame stackFrame : ((DSPThread) threadTreeNode.getObject()).getStackFrames()) {
                ThreadTreeNode stackFrameNode = new ThreadTreeNode(stackFrame);
                threadTreeNode.add(stackFrameNode);
                treeNodes.add(stackFrameNode);
            }
        }

        public DSPThread[] getThreads() {
            return threads;
        }

        public Set<ThreadTreeNode> getTreeNodes() {
            return treeNodes;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ThreadTreeNode that = (ThreadTreeNode) o;
            return Objects.equals(object, that.object);
        }

        @Override
        public int hashCode() {
            return Objects.hash(object);
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
