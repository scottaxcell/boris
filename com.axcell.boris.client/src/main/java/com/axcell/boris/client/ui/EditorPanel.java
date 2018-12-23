package com.axcell.boris.client.ui;

import com.axcell.boris.client.debug.dsp.*;
import com.axcell.boris.client.debug.event.DebugEvent;
import com.axcell.boris.client.debug.event.DebugEventListener;
import com.axcell.boris.client.debug.model.Breakpoint;
import com.axcell.boris.client.ui.event.GUIEvent;
import com.axcell.boris.client.ui.event.GUIEventListener;
import com.google.common.base.Strings;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 * update textpane each time debugger stops
 * remove bold when debugger terminates
 */
public class EditorPanel extends JPanel implements DebugEventListener, GUIEventListener {
    private String[] contents;
    private JTextPane textPane;
    private GdbDebugTarget client;
    private Long currentDebugLineNumber;

    /**
     * FOR DEBUG/DEVELOPMENT
     */
    private static final String TEST_CASE_DIR = "/home/saxcell/dev/boris/testcases/threadexample";
    private static final String SOURCE_FILENAME = String.format("%s/threadexample.cpp", TEST_CASE_DIR);
    private static final String TARGET_FILENAME = String.format("%s/threadexample", TEST_CASE_DIR);

    /**
     * END FOR DEBUG/DEVELOPMENT
     */

    public EditorPanel() {
        super(new BorderLayout());
        initContents();
        Boris.getDebugEventMgr().addListener(this);
        init();
    }

    private void initContents() {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(SOURCE_FILENAME))) {
            List<String> lines = new ArrayList<>();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }
            contents = lines.toArray(new String[lines.size()]);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateText(Long lineToBold) {
        textPane.setText("");
        Document document = textPane.getDocument();
        for (int i = 0; i < contents.length; i++) {
            if (lineToBold != null && lineToBold == (i + 1)) {
                SimpleAttributeSet attributeSet = new SimpleAttributeSet();
                attributeSet.addAttribute(StyleConstants.CharacterConstants.Bold, Boolean.TRUE);
                try {
                    document.insertString(document.getLength(), contents[i] + "\n", attributeSet);
                }
                catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
            else {
                SimpleAttributeSet attributeSet = new SimpleAttributeSet();
                try {
                    document.insertString(document.getLength(), contents[i] + "\n", attributeSet);
                }
                catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void init() {
        setBorder(BorderFactory.createTitledBorder("Editor"));

        textPane = new JTextPane();
        textPane.setEditable(false);

        textPane.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    try {
                        Point point = e.getPoint();
                        Document doc = textPane.getDocument();
                        int pos = textPane.viewToModel(point);
                        int rowStart = Utilities.getRowStart(textPane, pos);
                        int rowEnd = Utilities.getRowEnd(textPane, pos);
                        String lineText = doc.getText(pos, rowEnd - rowStart);
                        if (Strings.isNullOrEmpty(lineText))
                            return;

                        Long lineNumber = Long.valueOf(doc.getDefaultRootElement().getElementIndex(pos) + 1);
                        DSPBreakpoint newBreakpoint = new DSPBreakpoint(Paths.get(SOURCE_FILENAME), lineNumber, true);

                        Breakpoint[] breakpoints = Boris.getGlobalBreakpointMgr().getBreakpoints();
                        for (Breakpoint breakpoint : breakpoints) {
                            if (newBreakpoint.equals(breakpoint)) {
                                Boris.getGlobalBreakpointMgr().removeBreakpoint(breakpoint);
                                return;
                            }
                        }

                        Boris.getGlobalBreakpointMgr().addBreakpoint(newBreakpoint);
                    }
                    catch (BadLocationException e1) {
                        e1.printStackTrace();
                    }
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
        });

        updateText(null);

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void handleEvent(DebugEvent event) {
        /* TODO turn on when ready
        if (event.getType() == DebugEvent.STOPPED) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    if (client != null)
                        currentDebugLineNumber = client.getThreads()[0].getTopStackFrame().getLineNumber();
                    else
                        currentDebugLineNumber = null;
                    return true;
                }

                @Override
                protected void done() {
                    SwingUtilities.invokeLater(() -> {
                        updateText(currentDebugLineNumber);
                    });
                }
            };
            worker.execute();
        }
        else if (event.getType() == DebugEvent.EXITED
                || event.getType() == DebugEvent.TERMINATED) {
            currentDebugLineNumber = null;
            SwingUtilities.invokeLater(() -> {
                updateText(currentDebugLineNumber);
            });
        }
        */
    }

    public GdbDebugTarget getClient() {
        return client;
    }

    public void setClient(GdbDebugTarget client) {
        this.client = client;
    }

    @Override
    public void handleEvent(GUIEvent event) {
        /* TODO turn on when ready
        if (event.getType() == GUIEvent.THREAD_SELECTED) {
            if (event.getObject() instanceof DSPThread) {
                DSPThread thread = (DSPThread) event.getObject();
                Long lineNumber = thread.getTopStackFrame().getLineNumber();
                currentDebugLineNumber = lineNumber;
                SwingUtilities.invokeLater(() -> {
                    updateText(currentDebugLineNumber);
                });
            }
        }
        else if (event.getType() == GUIEvent.STACK_FRAME_SELECTED) {
            if (event.getObject() instanceof DSPStackFrame) {
                DSPStackFrame stackFrame = (DSPStackFrame) event.getObject();
                Long lineNumber = stackFrame.getLineNumber();
                currentDebugLineNumber = lineNumber;
                SwingUtilities.invokeLater(() -> {
                    updateText(currentDebugLineNumber);
                });
            }
        }
        */
    }
}
