package com.axcell.boris.dap.client.ui;

import com.axcell.boris.dap.client.debug.model.Breakpoint;
import com.axcell.boris.dap.client.debug.model.BreakpointListener;
import com.axcell.boris.dap.client.debug.model.BreakpointMgr;
import com.axcell.boris.dap.client.ui.event.GUIEvent;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class BreakpointsPanel extends JPanel implements BreakpointListener {
    private BreakpointMgr breakpointMgr;
    private JTable table;
    private BreakpointsTableModel model;

    public BreakpointsPanel(BreakpointMgr breakpointMgr) {
        super(new BorderLayout());
        this.breakpointMgr = breakpointMgr;
        init();
    }

    private void init() {
        setBorder(BorderFactory.createTitledBorder("Breakpoints"));
        model = new BreakpointsTableModel();
        table = new JTable(model);
        table.addMouseListener(new BreakpointsTableListener());
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    Breakpoint[] getBreakpoints() {
        return breakpointMgr.getBreakpoints();
    }

    @Override
    public void breakpointAdded(Breakpoint breakpoint) {
        if (model != null)
            model.fireTableDataChanged();
    }

    @Override
    public void breakpointChanged(Breakpoint breakpoint) {
        if (model != null)
            model.fireTableDataChanged();
    }

    @Override
    public void breakpointRemoved(Breakpoint breakpoint) {
        if (model != null)
            model.fireTableDataChanged();
    }

    private class BreakpointsTableModel extends AbstractTableModel {
        private String[] columnNames = new String[]{"Enabled", "Breakpoint"};
        private Class[] columnClasses = new Class[]{Boolean.class, String.class};

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
                return getBreakpoints()[rowIndex].isEnabled();
            }
            else {
                String value = String.format("%s [line: %s]", getBreakpoints()[rowIndex].getPath().getFileName(), String.valueOf(getBreakpoints()[rowIndex].getLineNumber()));
                return value;
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Breakpoint breakpoint = getBreakpoints()[rowIndex];
            breakpointMgr.setBreakpointEnabled(breakpoint, (Boolean) aValue);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnClasses[columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }
    }

    private class BreakpointsTableListener implements MouseListener {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                Point point = e.getPoint();
                int rowIndex = table.rowAtPoint(point);
                Breakpoint breakpoint = getBreakpoints()[rowIndex];
                breakpointMgr.removeBreakpoint(breakpoint);
                Boris.getGuiEventMgr().fireEvent(new GUIEvent(GUIEvent.BREAKPOINT_REMOVED, BreakpointsPanel.this));
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }
    }
}
