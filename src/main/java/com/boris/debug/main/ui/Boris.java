package com.boris.debug.main.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Boris {
    /**
     * TODO
     * menubar (open file, run exe, debug exe, exit)
     * editor
     * breakpoints
     * varables
     * threads/stacks
     * console
     */
    private JFrame frame;
    private JMenuBar menuBar;
    private JToolBar toolBar;
    private Container contentPane;
    private EditorPanel editorPanel;
    private BreakpointsPanel breakpointsPanel;
    private VariablesPanel variablesPanel;

    public Boris() {
        SwingUtilities.invokeLater(() -> initGui());
    }

    private void initMenuBar() {
        menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");

        JMenuItem openFileMenuItem = new JMenuItem("Open Source..");
        openFileMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(frame, "IMPLEMENT ME");
            }
        });
        fileMenu.add(openFileMenuItem);

        fileMenu.addSeparator();

        JMenuItem exitMenuItem = new JMenuItem("Exit..");
        exitMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        JMenu runMenu = new JMenu("Run");

        JMenuItem runAppMenuItem = new JMenuItem("Run App..");
        runAppMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(frame, "IMPLEMENT ME");
            }
        });
        runMenu.add(runAppMenuItem);

        JMenuItem debugAppMenuItem = new JMenuItem("Debug App..");
        debugAppMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(frame, "IMPLEMENT ME");
            }
        });
        runMenu.add(debugAppMenuItem);

        menuBar.add(runMenu);

        frame.setJMenuBar(menuBar);
    }

    private void initToolBar() {
        toolBar = new JToolBar();

        JButton runAppButton = new JButton("Run App");
        runAppButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(frame, "IMPLEMENT ME");

            }
        });
        toolBar.add(runAppButton);

        JButton debugAppButton = new JButton("Debug App");
        debugAppButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(frame, "IMPLEMENT ME");
            }
        });

        toolBar.add(debugAppButton);

        contentPane.add(toolBar, BorderLayout.NORTH);
    }

    private void initGui() {
        frame = new JFrame("Boris -- GDB Debugger Prototype");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initMenuBar();

        contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout());

        initToolBar();

        editorPanel = new EditorPanel();
        contentPane.add(editorPanel, BorderLayout.CENTER);

        breakpointsPanel = new BreakpointsPanel();
        contentPane.add(breakpointsPanel, BorderLayout.WEST);

        variablesPanel = new VariablesPanel();
        contentPane.add(variablesPanel, BorderLayout.EAST);

        frame.setSize(new Dimension(720, 550));
//        frame.pack();
        frame.setVisible(true);
    }
}
