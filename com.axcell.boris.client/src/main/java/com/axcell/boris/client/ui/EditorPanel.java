package com.axcell.boris.client.ui;

import com.axcell.boris.client.GdbDebugClient;
import com.axcell.boris.client.debug.dsp.DSPBreakpoint;
import com.axcell.boris.client.debug.event.DebugEvent;
import com.axcell.boris.client.debug.event.DebugEventListener;
import com.axcell.boris.client.debug.model.Breakpoint;
import com.axcell.boris.utils.Utils;
import com.google.common.base.Strings;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Utilities;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;

public class EditorPanel extends JPanel implements DebugEventListener {
    private JEditorPane editorPane;
    private GdbDebugClient client;

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
        init();
    }

    private void init() {
        setBorder(BorderFactory.createTitledBorder("Editor"));

        editorPane = new JEditorPane();
        editorPane.setEditable(false);

        editorPane.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    try {
                        Point point = e.getPoint();
                        Document doc = editorPane.getDocument();
                        int pos = editorPane.viewToModel(point);
                        int rowStart = Utilities.getRowStart(editorPane, pos);
                        int rowEnd = Utilities.getRowEnd(editorPane, pos);
                        String lineText = doc.getText(pos, rowEnd - rowStart);
                        if (Strings.isNullOrEmpty(lineText))
                            return;

                        Long lineNumber = Long.valueOf(doc.getDefaultRootElement().getElementIndex(pos) + 1);
                        DSPBreakpoint newBreakpoint = new DSPBreakpoint(Paths.get(SOURCE_FILENAME), lineNumber, true);

                        Breakpoint[] breakpoints = Boris.getGlobalBreakpointMgr().getBreakpoints();
                        for (Breakpoint breakpoint : breakpoints) {
                            if (newBreakpoint.equals(breakpoint)) {
//                                Boris.getGlobalBreakpointMgr().setBreakpointEnabled(breakpoint, !breakpoint.isEnabled());
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
        try {
            Reader reader = new FileReader(SOURCE_FILENAME);
            editorPane.read(reader, null);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void handleEvent(DebugEvent event) {
        if (event.getType() == DebugEvent.STOPPED) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    // TODO get line number from current frame
//                    client.getStackFrames();
                    return true;
                }

                @Override
                protected void done() {
                    // TODO paint label or something on to appropriate line to denote debugger has stopped here
                    Document doc = editorPane.getDocument();
                    for (int position = 0; position < doc.getLength(); position++) {
                        Long lineNumber = Long.valueOf(doc.getDefaultRootElement().getElementIndex(position) + 1);
//                        if (lineNumber == line number from stackframe) {
//                           paint something on line
//                           return;
//                        }
                    }
                }
            };
//            worker.execute();
        }
    }

    public GdbDebugClient getClient() {
        return client;
    }

    public void setClient(GdbDebugClient client) {
        this.client = client;
    }
}
