package com.axcell.boris.client.ui;

import com.axcell.boris.client.debug.dsp.*;
import com.axcell.boris.client.debug.event.DebugEvent;
import com.axcell.boris.client.debug.event.DebugEventListener;
import com.axcell.boris.client.ui.event.GUIEvent;
import com.axcell.boris.client.ui.event.GUIEventListener;
import com.axcell.boris.utils.Utils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class VariablesPanel extends JPanel implements DebugEventListener, GUIEventListener {
    private JTable table;
    private VariablesTableModel model;
    private GdbDebugTarget debugTarget;

    public VariablesPanel() {
        super(new BorderLayout());
        Boris.getDebugEventMgr().addListener(this);
        Boris.getGuiEventMgr().addListener(this);
        init();
    }

    private void init() {
        setBorder(BorderFactory.createTitledBorder("Variables"));

        model = new VariablesTableModel();
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public GdbDebugTarget getDebugTarget() {
        return debugTarget;
    }

    public void setDebugTarget(GdbDebugTarget debugTarget) {
        this.debugTarget = debugTarget;
    }

    @Override
    public void handleEvent(DebugEvent event) {
        if (event.getType() == DebugEvent.STOPPED) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    model.updateModel();
                    return true;
                }

                @Override
                protected void done() {
                    SwingUtilities.invokeLater(() -> {
                        model.fireTableDataChanged();
                    });
                }
            };
            worker.execute();
        }
    }

    @Override
    public void handleEvent(GUIEvent event) {
        if (event.getType() == GUIEvent.THREAD_SELECTED) {
//            if (event.getObject() instanceof DSPThread) {
//                DSPThread thread = (DSPThread) event.getObject();
//                DSPVariable[] variables = (DSPVariable[]) thread.getTopStackFrame().getVariables();
//                model.updateModel(variables);
//                SwingUtilities.invokeLater(() -> {
//                    model.fireTableDataChanged();
//                });
//            }
            model.testThings();
        }
        else if (event.getType() == GUIEvent.STACK_FRAME_SELECTED) {
//            if (event.getObject() instanceof DSPStackFrame) {
//                DSPStackFrame stackFrame = (DSPStackFrame) event.getObject();
//                DSPThread thread = (DSPThread) stackFrame.getThread();
//                DSPVariable[] variables = (DSPVariable[]) thread.getTopStackFrame().getVariables();
//                model.updateModel(variables);
//                SwingUtilities.invokeLater(() -> {
//                    model.fireTableDataChanged();
//                });
//            }
            model.testThings();
        }
    }

    private class VariablesTableModel extends AbstractTableModel {
        private String[] columnNames = new String[]{"Name", "Value"};
        private List<DSPVariable> variables = new ArrayList<>();

        void addVariables(DSPVariable[] variables) {
            for (DSPVariable variable : variables)
                addVariable(variable);
        }

        void addVariable(DSPVariable variable) {
            variables.add(variable);
        }

        @Override
        public int getRowCount() {
            return variables.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0)
                return variables.get(rowIndex).getName();
            else
                return variables.get(rowIndex).getValue();
        }

        void updateModel() {
            if (debugTarget != null) {
                variables.clear();
                Utils.debug("VariablesPanel: debugTarget.getVariables()");
                DSPVariable[] vars = debugTarget.getVariables();
                addVariables(vars);
            }
        }

        void updateModel(DSPVariable[] variables) {
            this.variables.clear();
            addVariables(variables);
        }

        void testThings() {
            DSPThread[] threads = debugTarget.getThreads();
            for (DSPThread thread : threads) {
                DSPStackFrame stackFrame = thread.getTopStackFrame();
                DSPVariable[] variables = stackFrame.getVariables();
                for (DSPVariable variable : variables) {
                    DSPValue value = variable.getValue();
                    Utils.debug("getValueString: " + value.getValueString());
                    Utils.debug("hasVariables: " + value.hasVariables());
                    if (value.hasVariables()) {
                        DSPVariable[] vs = value.getVariables();
                        for (DSPVariable v : vs) {
                            Utils.debug("v.getName: " + v.getName());
                            Utils.debug("v.geValue: " + v.getValue());
                            DSPValue val = v.getValue();
                            Utils.debug("val.getValueString: " + val.getValueString());
                        }
                    }
                }
            }
        }

    }
}
