package com.boris.debug.main.ui;

import com.boris.debug.client.GdbDebugClient;
import com.boris.debug.main.event.DebugEvent;
import com.boris.debug.main.event.DebugEventListener;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class VariablesPanel extends JPanel implements DebugEventListener {
    JTable table;
    VariablesTableModel model;
    GdbDebugClient client;

    public VariablesPanel() {
        super(new BorderLayout());
        init();
    }

    private void init() {
        setBorder(BorderFactory.createTitledBorder("Variables"));

        model = new VariablesTableModel();
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void addVariable(Variable variable) {
        if (model != null) {
            model.addVariable(variable);
        }
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
            // TODO ask client for variables
        }
    }

    public static class Variable {
        String name;
        String value;

        public Variable(String name, String variable) {
            this.name = name;
            this.value = variable;
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
        List<Variable> variables = new ArrayList<>();

        void addVariable(Variable variable) {
            variables.add(variable);
            fireTableDataChanged();
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
            if (columnIndex == 0) {
                return variables.get(rowIndex).getName();
            }
            else {
                return variables.get(rowIndex).getValue();
            }
        }
    }
}
