package com.boris.debug.main.ui;

import com.boris.debug.client.GdbDebugClient;
import com.boris.debug.main.event.DebugEventMgr;
import com.boris.debug.client.DSPBreakpoint;
import com.boris.debug.main.model.BreakpointMgr;
import com.boris.debug.main.model.IBreakpointListener;
import com.boris.debug.main.ui.event.GUIEventMgr;
import com.boris.debug.server.Target;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Paths;

public class Boris {
    /**
     * COMPONENTS
     * menubar (open file, run exe, debug exe, exit)
     * editor
     * breakpoints
     * varables
     * threads/stacks
     * console
     * <p>
     * TODO
     * launch debug process somehow, maybe a DebugSession holds a gdbdebugclient
     * - client should take target
     * - initialize
     * - setBreakpoints
     * - launch
     */

    private JFrame frame;
    private JMenuBar menuBar;
    private JToolBar toolBar;
    private Container contentPane;
    private EditorPanel editorPanel;
    private BreakpointsPanel breakpointsPanel;
    private VariablesPanel variablesPanel;
    private ConsolePanel consolePanel;

    private static BreakpointMgr breakpointMgr = new BreakpointMgr();
    private static GUIEventMgr guiEventMgr = new GUIEventMgr();
    private static DebugEventMgr debugEventMgr = new DebugEventMgr();

    private GdbDebugClient client;

    /**
     * FOR DEBUG/DEVELOPMENT
     */
    private static final String TEST_CASE_DIR = "/home/saxcell/dev/boris/testcases/helloworld";
    private static final String SOURCE_FILENAME = String.format("%s/helloworld.cpp", TEST_CASE_DIR);
    private static final String TARGET_FILENAME = String.format("%s/helloworld", TEST_CASE_DIR);
    /**
     * END FOR DEBUG/DEVELOPMENT
     */

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
                debugTarget();
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
                debugTarget();
            }
        });
        toolBar.add(debugAppButton);

        JButton continueAppButton = new JButton("Continue");
        continueAppButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (client != null) {
                    client.continueAllThreads();
                }
            }
        });
        toolBar.add(continueAppButton);

        JButton bogusBreakpointsButton = new JButton("Add Breakpoints");
        bogusBreakpointsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addBogusBreakpoints();
            }
        });
        toolBar.add(bogusBreakpointsButton);

        JButton bogusVariablesButton = new JButton("Add Variables");
        bogusBreakpointsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addBogusVariables();
            }
        });
        toolBar.add(bogusVariablesButton);

        contentPane.add(toolBar, BorderLayout.NORTH);
    }

    private void addBogusBreakpoints() {
        breakpointMgr.addBreakpoint(new DSPBreakpoint(Paths.get("/some/bogus/file.cpp"), 10L, true));
        breakpointMgr.addBreakpoint(new DSPBreakpoint(Paths.get("/some/bogus/file.cpp"), 32L, true));
        breakpointMgr.addBreakpoint(new DSPBreakpoint(Paths.get("/some/bogus/file/named/foo.cpp"), 4394L, false));
    }

    private void addBogusVariables() {
        variablesPanel.addVariable(new VariablesPanel.Variable("count", "1"));
        variablesPanel.addVariable(new VariablesPanel.Variable("isEarthFlat", "false"));
        variablesPanel.addVariable(new VariablesPanel.Variable("numElephants", "32"));
        variablesPanel.addVariable(new VariablesPanel.Variable("nameOfCoffee", "Kenyan"));
    }

    private void initGui() {
        frame = new JFrame("Boris");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initMenuBar();

        contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout());

        initToolBar();

        editorPanel = new EditorPanel();
        contentPane.add(editorPanel, BorderLayout.CENTER);

        breakpointsPanel = new BreakpointsPanel(getBreakpointMgr());
        addBreakpointListener(breakpointsPanel);
        contentPane.add(breakpointsPanel, BorderLayout.WEST);

        variablesPanel = new VariablesPanel();
        contentPane.add(variablesPanel, BorderLayout.EAST);

        consolePanel = new ConsolePanel();
        contentPane.add(consolePanel, BorderLayout.SOUTH);

        frame.setSize(new Dimension(1200, 720));
//        frame.pack();
        frame.setVisible(true);
    }

    private void debugTarget() {
        getBreakpointMgr().addBreakpoint(new DSPBreakpoint(Paths.get(SOURCE_FILENAME), 8L, true));
        Target target = new Target(TARGET_FILENAME);
        client = new GdbDebugClient(target, getBreakpointMgr());
        client.initialize(42);
    }

    public static BreakpointMgr getBreakpointMgr() {
        return breakpointMgr;
    }

    public void addBreakpointListener(IBreakpointListener listener) {
        getBreakpointMgr().addBreakpointListener(listener);
    }

    public void removeBreakpointListener(IBreakpointListener listener) {
        getBreakpointMgr().removeBreakpointListener(listener);
    }

    public static DebugEventMgr getDebugEventMgr() {
        return debugEventMgr;
    }

    public static GUIEventMgr getGuiEventMgr() {
        return guiEventMgr;
    }
}