package com.boris.debug.main.ui;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;

public class VariablesPanel extends JPanel {

    public VariablesPanel() {
        super(new BorderLayout());
        init();
    }

    private void init() {
        setBorder(BorderFactory.createTitledBorder("Variables"));
        setMinimumSize(new Dimension(200, 320));
    }
}
