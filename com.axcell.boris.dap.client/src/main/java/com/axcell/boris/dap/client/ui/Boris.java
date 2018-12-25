package com.axcell.boris.dap.client.ui;

import bibliothek.gui.DockController;
import bibliothek.gui.dock.DefaultDockable;
import bibliothek.gui.dock.SplitDockStation;
import bibliothek.gui.dock.station.split.SplitDockGrid;
import com.axcell.boris.dap.client.debug.dsp.DSPThread;
import com.axcell.boris.dap.client.debug.dsp.GDBDebugTarget;
import com.axcell.boris.dap.client.debug.event.DebugEvent;
import com.axcell.boris.dap.client.debug.event.DebugEventListener;
import com.axcell.boris.dap.client.debug.event.DebugEventMgr;
import com.axcell.boris.dap.client.debug.model.BreakpointListener;
import com.axcell.boris.dap.client.debug.model.GlobalBreakpointMgr;
import com.axcell.boris.dap.client.ui.event.GUIEventMgr;
import com.axcell.boris.dap.gdb.Target;
import com.axcell.boris.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;
import java.util.stream.Stream;

public class Boris implements DebugEventListener {
    /**
     * FOR DEBUG/DEVELOPMENT
     */
    public static final String TEST_CASE_DIR = "/home/saxcell/dev/boris/testcases/threadexample";
    public static final String SOURCE_FILENAME = String.format("%s/threadexample.cpp", TEST_CASE_DIR);
    public static final String TARGET_FILENAME = String.format("%s/threadexample", TEST_CASE_DIR);
    private static GlobalBreakpointMgr globalBreakpointMgr;
    private static GUIEventMgr guiEventMgr;
    private static DebugEventMgr debugEventMgr;
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
    private GDBDebugTarget debugTarget;

    /**
     * END FOR DEBUG/DEVELOPMENT
     */

    public Boris() {
        DebugEventMgr.getInstance().addListener(this);
        SwingUtilities.invokeLater(() -> initDockableGui());
    }

    public static GlobalBreakpointMgr getGlobalBreakpointMgr() {
        return globalBreakpointMgr.getInstance();
    }

    public static DebugEventMgr getDebugEventMgr() {
        return debugEventMgr.getInstance();
    }

    public static GUIEventMgr getGuiEventMgr() {
        return guiEventMgr.getInstance();
    }

    private void initMenuBar() {
        menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");

        JMenuItem openFileMenuItem = new JMenuItem("Open Source..");
        openFileMenuItem.addActionListener(e -> JOptionPane.showMessageDialog(frame, "IMPLEMENT ME"));
        fileMenu.add(openFileMenuItem);

        fileMenu.addSeparator();

        JMenuItem exitMenuItem = new JMenuItem("Exit..");
        exitMenuItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        JMenu runMenu = new JMenu("Run");

        JMenuItem runTargetMenuItem = new JMenuItem("Run Target..");
        runTargetMenuItem.addActionListener(e -> JOptionPane.showMessageDialog(frame, "IMPLEMENT ME"));
        runMenu.add(runTargetMenuItem);

        JMenuItem debugTargetMenuItem = new JMenuItem("Debug Target..");
        debugTargetMenuItem.addActionListener(e -> debugTarget());
        runMenu.add(debugTargetMenuItem);

        menuBar.add(runMenu);

        frame.setJMenuBar(menuBar);
    }

    private void initToolBar() {
        toolBar = new JToolBar();

        JButton runTargetButton = new JButton("Run Target");
        runTargetButton.addActionListener(e -> JOptionPane.showMessageDialog(frame, "IMPLEMENT ME"));
        toolBar.add(runTargetButton);

        JButton debugTargetButton = new JButton("Debug Target");
        debugTargetButton.addActionListener(e -> debugTarget());
        toolBar.add(debugTargetButton);

        JButton nextButton = new JButton("Step Over");
        nextButton.addActionListener(e -> stepOver());
        toolBar.add(nextButton);

        JButton stepInButton = new JButton("Step In");
        stepInButton.addActionListener(e -> stepInto());
        toolBar.add(stepInButton);

        JButton stepReturnButton = new JButton("Step Out");
        stepReturnButton.addActionListener(e -> stepReturn());
        toolBar.add(stepReturnButton);

        JButton continueTargetButton = new JButton("Resume");
        continueTargetButton.addActionListener(e -> resume());
        toolBar.add(continueTargetButton);

        JButton pauseButton = new JButton("Pause");
        pauseButton.addActionListener(e -> pause());
        toolBar.add(pauseButton);

        JButton terminateTargetButton = new JButton("Stop");
        terminateTargetButton.addActionListener(e -> stopTarget());
        toolBar.add(terminateTargetButton);

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

        variablesPanel = new VariablesPanel();
        DefaultDockable variablesDockable = new DefaultDockable();
        variablesDockable.add(variablesPanel);
        grid.addDockable(2, 0, 1, 1, variablesDockable);

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
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                Target target = new Target(TARGET_FILENAME);
                debugTarget = new GDBDebugTarget(target, getGlobalBreakpointMgr());
                threadsPanel.setDebugTarget(debugTarget);
                editorPanel.setDebugTarget(debugTarget);
                debugTarget.initialize(42);
                return true;
            }
        };
        worker.execute();
    }

    public void addBreakpointListener(BreakpointListener listener) {
        getGlobalBreakpointMgr().addBreakpointListener(listener);
    }

    public void removeBreakpointListener(BreakpointListener listener) {
        getGlobalBreakpointMgr().removeBreakpointListener(listener);
    }

    @Override
    public void handleEvent(DebugEvent event) {
        if (event.getType() == DebugEvent.EXITED || event.getType() == DebugEvent.TERMINATED) {
            Utils.debug("Boris killed GDB debugTarget");
            debugTarget = null;
        }
    }

    public void stepOver() {
        if (debugTarget == null || !debugTarget.isSuspended()) {
            JOptionPane.showMessageDialog(frame, "Debugger is not suspended or running..");
            return;
        }
        if (threadsPanel != null) {
            Optional<DSPThread> thread = threadsPanel.getSelectedThread();
            thread.ifPresentOrElse(DSPThread::stepOver, () -> stepOverAllThreads());
        }
        else
            stepOverAllThreads();
    }

    public void stepOverAllThreads() {
        Stream.of(debugTarget.getThreads())
                .forEach(DSPThread::stepOver);
    }

    public void resume() {
        if (debugTarget == null || !debugTarget.isSuspended()) {
            JOptionPane.showMessageDialog(frame, "Debugger is not suspended or running..");
            return;
        }
        debugTarget.resume();
    }

    private void stopTarget() {
        JOptionPane.showMessageDialog(frame, "IMPLEMENT ME");
        return;
//        if (debugTarget != null)
//            debugTarget.disconnect();
    }

    public void stepInto() {
        if (debugTarget == null || !debugTarget.isSuspended()) {
            JOptionPane.showMessageDialog(frame, "Debugger is not suspended or running..");
            return;
        }
        if (threadsPanel != null) {
            Optional<DSPThread> thread = threadsPanel.getSelectedThread();
            thread.ifPresent(DSPThread::stepInto);
        }
    }

    public void stepReturn() {
        if (debugTarget == null || !debugTarget.isSuspended()) {
            JOptionPane.showMessageDialog(frame, "Debugger is not suspended or running..");
            return;
        }
        if (threadsPanel != null) {
            Optional<DSPThread> thread = threadsPanel.getSelectedThread();
            thread.ifPresent(DSPThread::stepReturn);
        }
    }

    private void pause() {
        if (debugTarget == null || !debugTarget.isSuspended()) {
            JOptionPane.showMessageDialog(frame, "Debugger is not suspended or running..");
            return;
        }
//        if (threadsPanel != null) {
//            Optional<DSPThread> thread = threadsPanel.getSelectedThread();
//            thread.ifPresent(DSPThread::stepRe);
//        }
    }
}
