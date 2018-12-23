package com.axcell.boris.client.ui;

import com.axcell.boris.client.debug.dsp.DSPStackFrame;
import com.axcell.boris.client.debug.dsp.DSPThread;
import com.axcell.boris.client.debug.dsp.DSPValue;
import com.axcell.boris.client.debug.dsp.DSPVariable;
import com.axcell.boris.client.debug.event.DebugEvent;
import com.axcell.boris.client.debug.event.DebugEventListener;
import com.axcell.boris.client.ui.event.GUIEvent;
import com.axcell.boris.client.ui.event.GUIEventListener;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class VariablesPanel extends JPanel implements DebugEventListener, GUIEventListener {
    private JTable table;
    private VariablesTableModel model;

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

    public void handleEvent(DebugEvent event) {
        if (event.getType() == DebugEvent.EXITED || event.getType() == DebugEvent.TERMINATED) {
            model.cleanModel();
        }
    }

    @Override
    public void handleEvent(GUIEvent event) {
        if (event.getType() == GUIEvent.THREAD_SELECTED) {
            if (event.getObject() instanceof DSPThread) {
                DSPThread thread = (DSPThread) event.getObject();
                model.updateModel(thread);
            }
        }
        else if (event.getType() == GUIEvent.STACK_FRAME_SELECTED) {
            if (event.getObject() instanceof DSPStackFrame) {
                DSPStackFrame stackFrame = (DSPStackFrame) event.getObject();
                model.updateModel(stackFrame);
            }
        }
    }

    private static class VariablesTableRow {
        private String name;
        private String value;

        public VariablesTableRow(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    private class VariablesTableModel extends AbstractTableModel {
        private String[] columnNames = new String[]{"Name", "Value"};
        private List<VariablesTableRow> rows = new ArrayList();

        void updateModel(DSPStackFrame stackFrame) {
            rows.clear();
            SwingUtilities.invokeLater(() -> fireTableDataChanged());
            DSPVariable[] variables = stackFrame.getVariables();
            for (DSPVariable variable : variables) {
                DSPValue value = variable.getValue();
                if (value.hasVariables()) {
                    DSPVariable[] vars = value.getVariables();
                    for (DSPVariable var : vars) {
                        DSPValue val = var.getValue();
                        VariablesTableRow row = new VariablesTableRow(var.getName(), val.getValueString());
                        rows.add(row);
                        SwingUtilities.invokeLater(() -> fireTableRowsInserted(rows.size() - 1, rows.size() - 1));
                    }
                }
            }
        }

        void updateModel(DSPThread thread) {
            DSPStackFrame stackFrame = thread.getTopStackFrame();
            updateModel(stackFrame);
        }

        void cleanModel() {
            rows.clear();
            SwingUtilities.invokeLater(() -> fireTableDataChanged());
        }

        @Override
        public int getRowCount() {
            return rows.size();
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
                return rows.get(rowIndex).getName();
            else
                return rows.get(rowIndex).getValue();
        }
    }
}
