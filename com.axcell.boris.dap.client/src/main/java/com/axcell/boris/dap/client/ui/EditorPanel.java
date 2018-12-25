package com.axcell.boris.dap.client.ui;

import com.axcell.boris.dap.client.debug.dsp.DSPBreakpoint;
import com.axcell.boris.dap.client.debug.dsp.DSPStackFrame;
import com.axcell.boris.dap.client.debug.dsp.DSPThread;
import com.axcell.boris.dap.client.debug.dsp.GDBDebugTarget;
import com.axcell.boris.dap.client.debug.event.DebugEvent;
import com.axcell.boris.dap.client.debug.event.DebugEventListener;
import com.axcell.boris.dap.client.debug.model.Breakpoint;
import com.axcell.boris.dap.client.debug.model.StackFrame;
import com.axcell.boris.dap.client.ui.event.GUIEvent;
import com.axcell.boris.dap.client.ui.event.GUIEventListener;
import com.google.common.base.Strings;
import org.eclipse.lsp4j.debug.StoppedEventArguments;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

public class EditorPanel extends JPanel implements DebugEventListener, GUIEventListener {
    private JEditorPane editor;
    private GDBDebugTarget debugTarget;
    private Optional<Long> debuggerLineNumber = Optional.empty();

    public EditorPanel() {
        super(new BorderLayout());
        Boris.getDebugEventMgr().addListener(this);
        Boris.getGuiEventMgr().addListener(this);
        init();
    }

    public void setDebugTarget(GDBDebugTarget debugTarget) {
        this.debugTarget = debugTarget;
    }

    private void initContents() {
        try (Reader reader = new FileReader(Boris.SOURCE_FILENAME)) {
            editor.read(reader, null);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        editor = new JTextPane();
        editor.setEditable(false);
        editor.addMouseListener(new EditorMouseListener());
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));

        initContents();

        LineNumbersView lineNumbersView = new LineNumbersView(editor);

        JScrollPane scroll = new JScrollPane(editor);
        scroll.setRowHeaderView(lineNumbersView);
        add(scroll);
    }

    @Override
    public void handleEvent(DebugEvent event) {
        if (event.getType() == DebugEvent.STOPPED) {
            Long threadId = ((StoppedEventArguments) event.getObject()).getThreadId();
            Optional<DSPThread> thread = debugTarget.getThread(threadId);
            thread.ifPresentOrElse(this::setDebuggerLineNumber, () -> debuggerLineNumber = Optional.empty());
            highlightCurrentInstructionLine();
            SwingUtilities.invokeLater(this::repaint);
        }
        else if (event.getType() == DebugEvent.EXITED
                || event.getType() == DebugEvent.TERMINATED) {
            debuggerLineNumber = Optional.empty();
            highlightCurrentInstructionLine();
            SwingUtilities.invokeLater(this::repaint);
        }
    }

    private void setDebuggerLineNumber(DSPThread thread) {
        StackFrame stackFrame = thread.getTopStackFrame();
        debuggerLineNumber = stackFrame != null ? Optional.ofNullable(stackFrame.getLineNumber()) : Optional.empty();
    }

    private void highlightCurrentInstructionLine() {
        removeExistingHighlights();

        Position startPosition = editor.getDocument().getStartPosition();
        Position endPostion = editor.getDocument().getEndPosition();
        int startOffset = startPosition.getOffset();
        int endOffset = endPostion.getOffset();
        while (startOffset < endOffset) {
            try {
                Optional<Long> lineNumber = getLineNumber(startOffset);
                if (lineNumber.isPresent() && isCurrentDebuggerLine(lineNumber.get())) {
                    int rowStart = Utilities.getRowStart(editor, startOffset);
                    int rowEnd = Utilities.getRowEnd(editor, startOffset);
                    String lineText = editor.getDocument().getText(startOffset, rowEnd - rowStart);
                    if (!Strings.isNullOrEmpty(lineText)) {
                        Highlighter highlighter = editor.getHighlighter();
                        CurrentInstructionPointerHighlighter currentInstructionPointerHighlighter = new CurrentInstructionPointerHighlighter(Color.GREEN);
                        highlighter.addHighlight(rowStart, rowEnd, currentInstructionPointerHighlighter);
                        break;
                    }
                }
                startOffset = Utilities.getRowEnd(editor, startOffset) + 1;
            }
            catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    private void removeExistingHighlights() {
        Highlighter highlighter = editor.getHighlighter();
        Stream.of(editor.getHighlighter().getHighlights())
                .filter(highlight -> highlight.getPainter() instanceof CurrentInstructionPointerHighlighter)
                .forEach(highlight -> highlighter.removeHighlight(highlight));
    }

    private Optional<Long> getLineNumber(int offset) {
        Element root = editor.getDocument().getDefaultRootElement();
        int index = root.getElementIndex(offset);
        Element line = root.getElement(index);
        return line.getStartOffset() == offset ? Optional.ofNullable(Long.valueOf(index + 1)) : Optional.empty();
    }

    private boolean isCurrentDebuggerLine(Long lineNumber) {
        boolean result = false;
        if (debuggerLineNumber.isPresent())
            result = debuggerLineNumber.get().equals(lineNumber);
        return result;
    }

    @Override
    public void handleEvent(GUIEvent event) {
        if (event.getType() == GUIEvent.STACK_FRAME_SELECTED) {
            if (event.getObject() instanceof DSPStackFrame)
                scrollToSelectedStackFrame((DSPStackFrame) event.getObject());
        }
        else if (event.getType() == GUIEvent.BREAKPOINT_REMOVED) {
            repaint();
        }
    }

    private void scrollToSelectedStackFrame(DSPStackFrame stackFrame) {
        // TODO check source matches!
        // TODO highlight line very lightly
        Long stackFrameLineNumber = stackFrame.getLineNumber();
        Position startPosition = editor.getDocument().getStartPosition();
        Position endPostion = editor.getDocument().getEndPosition();
        int startOffset = startPosition.getOffset();
        int endOffset = endPostion.getOffset();
        while (startOffset < endOffset) {
            try {
                Optional<Long> lineNumber = getLineNumber(startOffset);
                if (lineNumber.isPresent() && lineNumber.get().equals(stackFrameLineNumber)) {
                    editor.scrollRectToVisible(editor.modelToView(startOffset));
                    break;
                }
                startOffset = Utilities.getRowEnd(editor, startOffset) + 1;
            }
            catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    private class EditorMouseListener implements MouseListener {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                try {
                    Point point = e.getPoint();
                    Document doc = editor.getDocument();
                    int pos = editor.viewToModel(point);
                    int rowStart = Utilities.getRowStart(editor, pos);
                    int rowEnd = Utilities.getRowEnd(editor, pos);
                    String lineText = doc.getText(pos, rowEnd - rowStart);
                    if (Strings.isNullOrEmpty(lineText))
                        return;

                    Long lineNumber = Long.valueOf(doc.getDefaultRootElement().getElementIndex(pos) + 1);
                    DSPBreakpoint newBreakpoint = new DSPBreakpoint(Paths.get(Boris.SOURCE_FILENAME), lineNumber, true);

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
    }

    private class LineNumbersView extends JComponent implements DocumentListener, CaretListener, ComponentListener {
        private static final int MARGIN_WIDTH = 28;
        private JTextComponent editor;
        private Font font;

        public LineNumbersView(JTextComponent editor) {
            this.editor = editor;

            editor.getDocument().addDocumentListener(this);
            editor.addComponentListener(this);
            editor.addCaretListener(this);
        }

        /**
         * Fetches y axis position for the line number for the element at the given offset
         */
        private int getOffsetY(int offset) throws BadLocationException {
            FontMetrics fontMetrics = editor.getFontMetrics(editor.getFont());
            int descent = fontMetrics.getDescent();
            Rectangle r = editor.modelToView(offset);
            int y = r.y + r.height - descent;
            return y;
        }

        private String getLineNumberForPainting(Long lineNumber) {
            return String.format("%3d", lineNumber);
        }

        private Color getLineNumberColor(Long lineNumber) {
            Optional<Breakpoint> breakpoint = Stream.of(Boris.getGlobalBreakpointMgr().getBreakpoints())
                    .filter(b -> lineNumber.equals(b.getLineNumber()))
                    .findFirst();
            return breakpoint.isPresent() ? Color.RED : Color.BLACK;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            Rectangle clip = g.getClipBounds();
            int startOffset = editor.viewToModel(new Point(0, clip.y));
            int endOffset = editor.viewToModel(new Point((0), clip.y + clip.height));
            while (startOffset <= endOffset) {
                try {
                    Optional<Long> lineNumber = getLineNumber(startOffset);
                    if (lineNumber.isPresent()) {
                        int x = getInsets().left + 2;
                        int y = getOffsetY(startOffset);
                        font = font != null ? font : new Font(Font.MONOSPACED, Font.BOLD, editor.getFont().getSize());
                        g.setFont(font);
                        g.setColor(getLineNumberColor(lineNumber.get()));
                        g.drawString(getLineNumberForPainting(lineNumber.get()), x, y);
                    }
                    startOffset = Utilities.getRowEnd(editor, startOffset) + 1;
                }
                catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void componentResized(ComponentEvent e) {
            updateSize();
            documentChanged();
        }

        private void documentChanged() {
            SwingUtilities.invokeLater(this::repaint);
        }

        private void updateSize() {
            Dimension size = new Dimension(MARGIN_WIDTH, editor.getHeight());
            setPreferredSize(size);
            setSize(size);
        }

        @Override
        public void componentMoved(ComponentEvent e) {
        }

        @Override
        public void componentShown(ComponentEvent e) {
            updateSize();
            documentChanged();
        }

        @Override
        public void componentHidden(ComponentEvent e) {
        }

        @Override
        public void caretUpdate(CaretEvent e) {
            documentChanged();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            documentChanged();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            documentChanged();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            documentChanged();
        }
    }

    private class CurrentInstructionPointerHighlighter extends DefaultHighlighter.DefaultHighlightPainter {

        /**
         * Constructs a new highlight painter. If <code>c</code> is null,
         * the JTextComponent will be queried for its selection color.
         *
         * @param c the color for the highlight
         */
        public CurrentInstructionPointerHighlighter(Color c) {
            super(c);
        }
    }
}