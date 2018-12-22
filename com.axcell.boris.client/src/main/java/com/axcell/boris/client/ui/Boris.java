package com.axcell.boris.client.ui;

import com.axcell.boris.client.debug.dsp.DSPBreakpoint;
import com.axcell.boris.client.debug.dsp.DSPThread;
import com.axcell.boris.client.GdbDebugClient;
import com.axcell.boris.client.debug.event.DebugEventMgr;
import com.axcell.boris.client.debug.model.BreakpointListener;
import com.axcell.boris.client.debug.model.GlobalBreakpointMgr;
import com.axcell.boris.client.debug.model.StackFrame;
import com.axcell.boris.client.ui.event.GUIEventMgr;
import com.axcell.boris.dap.gdb.Target;
import com.axcell.boris.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Paths;

public class Boris {
    /**
     * COMPONENTS
     * menubar (open file, run exe, dsp exe, exit)
     * editor
     * breakpoints
     * varables
     * threads/stacks
     * console
     * <p>
     * TODO
     * launch dsp process somehow, maybe a DebugSession holds a gdbdebugclient
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
    private ThreadsPanel threadsPanel;

    private static GlobalBreakpointMgr globalBreakpointMgr;
    private static GUIEventMgr guiEventMgr;
    private static DebugEventMgr debugEventMgr;

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

        JButton nextButton = new JButton("Next");
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (client != null) {
                    client.next();
                }
            }
        });
        toolBar.add(nextButton);

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

        JButton threadsButton = new JButton("Threads");
        threadsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                threads();
            }
        });
        toolBar.add(threadsButton);

        contentPane.add(toolBar);
    }

    private void addBogusBreakpoints() {
//        getGlobalBreakpointMgr().addBreakpoint(new DSPBreakpoint(Paths.get(SOURCE_FILENAME), 8L, true));
//        getGlobalBreakpointMgr().addBreakpoint(new DSPBreakpoint(Paths.get(SOURCE_FILENAME), 11L, false));
        getGlobalBreakpointMgr().addBreakpoint(new DSPBreakpoint(Paths.get(SOURCE_FILENAME), 13L, true));
    }

    private void addBogusVariables() {
//        variablesPanel.addVariable(new VariablesPanel.Variable("count", "1"));
//        variablesPanel.addVariable(new VariablesPanel.Variable("isEarthFlat", "false"));
//        variablesPanel.addVariable(new VariablesPanel.Variable("numElephants", "32"));
//        variablesPanel.addVariable(new VariablesPanel.Variable("nameOfCoffee", "Kenyan"));
    }

    private void initGui() {
        frame = new JFrame("Boris");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initMenuBar();

        contentPane = frame.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));

        initToolBar();

        breakpointsPanel = new BreakpointsPanel(getGlobalBreakpointMgr());
        breakpointsPanel.setPreferredSize(new Dimension(1200, 100));
        addBreakpointListener(breakpointsPanel);
        contentPane.add(breakpointsPanel);

        threadsPanel = new ThreadsPanel(client);
        threadsPanel.setPreferredSize(new Dimension(1200, 150));
        contentPane.add(threadsPanel);

        editorPanel = new EditorPanel();
        editorPanel.setPreferredSize(new Dimension(1200, 150));
        contentPane.add(editorPanel);

        variablesPanel = new VariablesPanel();
        variablesPanel.setPreferredSize(new Dimension(1200, 100));
        contentPane.add(editorPanel);
        contentPane.add(variablesPanel);

        consolePanel = new ConsolePanel();
        consolePanel.setPreferredSize(new Dimension(1200, 200));
        contentPane.add(consolePanel);

        frame.setSize(new Dimension(1200, 1000));
        frame.pack();
        frame.setVisible(true);
    }

    private void debugTarget() {
        Target target = new Target(TARGET_FILENAME);
        client = new GdbDebugClient(target, getGlobalBreakpointMgr());
        threadsPanel.setClient(client);
        variablesPanel.setClient(client);
        client.initialize(42);
    }

    private void threads() {
        if (client != null) {
            DSPThread[] threads = client.getThreads();
            Utils.out("threads = " + threads.length);
            for (DSPThread thread : threads) {
                StackFrame[] stackFrames = thread.getStackFrames();
                Utils.out("stackFrames = " + stackFrames.length);
            }
        }
    }
    public static GlobalBreakpointMgr getGlobalBreakpointMgr() {
        return globalBreakpointMgr.getInstance();
    }

    public void addBreakpointListener(BreakpointListener listener) {
        getGlobalBreakpointMgr().addBreakpointListener(listener);
    }

    public void removeBreakpointListener(BreakpointListener listener) {
        getGlobalBreakpointMgr().removeBreakpointListener(listener);
    }

    public static DebugEventMgr getDebugEventMgr() {
        return debugEventMgr.getInstance();
    }

    public static GUIEventMgr getGuiEventMgr() {
        return guiEventMgr.getInstance();
    }
}
