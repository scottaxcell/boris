package com.axcell.boris.client.ui;

import bibliothek.gui.DockController;
import bibliothek.gui.dock.DefaultDockable;
import bibliothek.gui.dock.SplitDockStation;
import bibliothek.gui.dock.station.split.SplitDockGrid;
import com.axcell.boris.client.debug.dsp.DSPThread;
import com.axcell.boris.client.debug.dsp.GdbDebugTarget;
import com.axcell.boris.client.debug.event.DebugEvent;
import com.axcell.boris.client.debug.event.DebugEventListener;
import com.axcell.boris.client.debug.event.DebugEventMgr;
import com.axcell.boris.client.debug.model.BreakpointListener;
import com.axcell.boris.client.debug.model.GlobalBreakpointMgr;
import com.axcell.boris.client.ui.event.GUIEventMgr;
import com.axcell.boris.dap.gdb.Target;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Boris implements DebugEventListener {
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
     * - debugTarget should take target
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

    private GdbDebugTarget debugTarget;

    /**
     * FOR DEBUG/DEVELOPMENT
     */
    private static final String TEST_CASE_DIR = "/home/saxcell/dev/boris/testcases/threadexample";
    private static final String SOURCE_FILENAME = String.format("%s/threadexample.cpp", TEST_CASE_DIR);
    private static final String TARGET_FILENAME = String.format("%s/threadexample", TEST_CASE_DIR);

    /**
     * END FOR DEBUG/DEVELOPMENT
     */

    public Boris() {
        DebugEventMgr.getInstance().addListener(this);
        SwingUtilities.invokeLater(() -> initDockableGui());
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

        JButton runAppButton = new JButton("Run Target");
        runAppButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(frame, "IMPLEMENT ME");

            }
        });
        toolBar.add(runAppButton);

        JButton debugAppButton = new JButton("Debug Target");
        debugAppButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                debugTarget();
            }
        });
        toolBar.add(debugAppButton);

        JButton nextButton = new JButton("Step Over");
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stepOver();
            }
        });
        toolBar.add(nextButton);

        JButton continueAppButton = new JButton("Resume");
        continueAppButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resume();
            }
        });
        toolBar.add(continueAppButton);

        contentPane.add(toolBar);
    }

    private void initDockableGui() {
        frame = new JFrame("Boris");

        initMenuBar();

        contentPane = frame.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));

        initToolBar();

        DockController controller = new DockController();
        SplitDockStation station = new SplitDockStation();
        controller.add(station);
        SplitDockGrid grid = new SplitDockGrid();

        threadsPanel = new ThreadsPanel();
        DefaultDockable threadsDockable = new DefaultDockable();
        threadsDockable.add(threadsPanel);
        grid.addDockable(0, 0, 1, 2, threadsDockable);

        editorPanel = new EditorPanel();
        DefaultDockable editorDockable = new DefaultDockable();
        editorDockable.add(editorPanel);
        grid.addDockable(1, 0, 1, 2, editorDockable);

//        variablesPanel = new VariablesPanel();
//        DefaultDockable variablesDockable = new DefaultDockable();
//        variablesDockable.add(variablesPanel);
//        grid.addDockable(2, 0, 1, 1, variablesDockable);

        breakpointsPanel = new BreakpointsPanel(getGlobalBreakpointMgr());
        addBreakpointListener(breakpointsPanel);
        DefaultDockable breakpointsDockable = new DefaultDockable();
        breakpointsDockable.add(breakpointsPanel);
        grid.addDockable(2, 1, 1, 1, breakpointsDockable);

        consolePanel = new ConsolePanel();
        DefaultDockable consoleDockable = new DefaultDockable();
        consoleDockable.add(consolePanel);
        grid.addDockable(0, 2, 3, 1, consoleDockable);

        station.dropTree(grid.toTree());

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        contentPane.add(station.getComponent());
        frame.setSize(new Dimension(1200, 1000));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void debugTarget() {
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                Target target = new Target(TARGET_FILENAME);
                debugTarget = new GdbDebugTarget(target, getGlobalBreakpointMgr());
                threadsPanel.setDebugTarget(debugTarget);
//                variablesPanel.setDebugTarget(debugTarget);
                editorPanel.setClient(debugTarget);
                debugTarget.initialize(42);
                return true;
            }
        };
        worker.execute();
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

    @Override
    public void handleEvent(DebugEvent event) {
        if (event.getType() == DebugEvent.EXITED || event.getType() == DebugEvent.TERMINATED) {
//            Utils.debug("Boris killed GDB debugTarget");
//            debugTarget = null;
        }
    }

    public void stepOver() {
        if (debugTarget == null || !debugTarget.isSuspended()) {
            JOptionPane.showMessageDialog(frame, "Debugger is not suspended or running..");
            return;
        }
        if (threadsPanel != null) {
            DSPThread thread = threadsPanel.getSelectedThread();
            if (thread != null) {
                thread.stepOver();
            }
        }
        else {
            for (DSPThread thread : debugTarget.getThreads()) {
                thread.stepOver();
            }
        }
    }

    public void resume() {
        if (debugTarget == null || !debugTarget.isSuspended()) {
            JOptionPane.showMessageDialog(frame, "Debugger is not suspended or running..");
            return;
        }
        debugTarget.resume();
    }
}
