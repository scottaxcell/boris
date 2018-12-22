package com.axcell.boris.client.ui;

import com.axcell.boris.client.GdbDebugClient;
import com.axcell.boris.client.debug.dsp.DSPVariable;
import com.axcell.boris.client.debug.event.DebugEvent;
import com.axcell.boris.client.debug.event.DebugEventListener;
import com.axcell.boris.utils.Utils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class VariablesPanel extends JPanel implements DebugEventListener {
    private JTable table;
    private VariablesTableModel model;
    private GdbDebugClient client;

    public VariablesPanel() {
        super(new BorderLayout());
        Boris.getDebugEventMgr().addListener(this);
        init();
    }

    private void init() {
        setBorder(BorderFactory.createTitledBorder("Variables"));

        model = new VariablesTableModel();
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);
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
                protected Boolean doInBackground() {
                    model.updateModel();
                    return true;
                }

                @Override
                protected void done() {
                    SwingUtilities.invokeLater(() -> {
                        StringBuilder sb = new StringBuilder("UPDATING variables panel..");
                        for (DSPVariable v : model.variables) {
                            sb.append(" Variable {" + v.getName() + " : " + v.getValue() + "}");
                        }
                        Utils.debug("SGA -- " + sb.toString());

                        model.fireTableDataChanged();
                    });
                }
            };
            worker.execute();
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
            if (client != null) {
                variables.clear();
                Utils.debug("VariablesPanel: client.getVariables()");
                DSPVariable[] vars = client.getVariables();
                addVariables(vars);
            }
        }
    }
}
