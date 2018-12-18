package com.boris.debug.main.ui;

import com.boris.debug.main.model.IBreakpoint;
import com.boris.debug.main.model.IBreakpointListener;
import com.boris.debug.main.model.IBreakpointMgr;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;

public class BreakpointsPanel extends JPanel implements IBreakpointListener {
    private IBreakpointMgr breakpointMgr;
    private JTable table;
    private BreakpointsTableModel model;

    public BreakpointsPanel(IBreakpointMgr breakpointMgr) {
        super(new BorderLayout());

        this.breakpointMgr = breakpointMgr;

        init();
    }

    public void init() {
        setBorder(BorderFactory.createTitledBorder("Breakpoints"));

        model = new BreakpointsTableModel();
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    IBreakpoint[] getBreakpoints() {
        return breakpointMgr.getBreakpoints();
    }

    @Override
    public void breakpointAdded(IBreakpoint breakpoint) {
        if (model != null)
            model.fireTableDataChanged();
    }

    @Override
    public void breakpointChanged(IBreakpoint breakpoint) {
        if (model != null)
            model.fireTableDataChanged();
    }

    @Override
    public void breakpointRemoved(IBreakpoint breakpoint) {
        if (model != null)
            model.fireTableDataChanged();
    }

    private class BreakpointsTableModel extends AbstractTableModel {
        private String[] columnNames = new String[]{"Breakpoint", "Enabled"};
        private Class[] columnClasses = new Class[]{String.class, Boolean.class};

        @Override
        public int getRowCount() {
            return getBreakpoints().length;
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
                String value = String.format("%s [line: %s]", getBreakpoints()[rowIndex].getPath().getFileName(), String.valueOf(getBreakpoints()[rowIndex].getLineNumber()));
                return value;
            }
            else {
                return getBreakpoints()[rowIndex].isEnabled();
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            getBreakpoints()[rowIndex].setEnabled((boolean) aValue);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnClasses[columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1;
        }
    }

}
