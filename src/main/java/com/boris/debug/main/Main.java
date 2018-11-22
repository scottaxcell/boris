package com.boris.debug.main;

import com.boris.debug.client.ui.BreakpointsPanel;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) {
        System.out.println("Hello World!");
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("GDB Debugger Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JComponent breakpointsPanel = new BreakpointsPanel();
        breakpointsPanel.setOpaque(true);
        frame.setContentPane(breakpointsPanel);

        breakpointsPanel.setMinimumSize(new Dimension(breakpointsPanel.getPreferredSize().width, 100));

        frame.pack();
        frame.setVisible(true);
    }
}
