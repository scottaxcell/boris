package com.axcell.boris.dap.client.ui;

import com.axcell.boris.dap.client.debug.dsp.DSPBreakpoint;
import com.axcell.boris.dap.client.debug.dsp.DSPThread;
import com.axcell.boris.dap.client.debug.dsp.GDBDebugTarget;
import com.axcell.boris.dap.client.debug.event.DebugEvent;
import com.axcell.boris.dap.client.debug.event.DebugEventListener;
import com.axcell.boris.dap.client.debug.model.Breakpoint;
import com.axcell.boris.dap.client.debug.model.StackFrame;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EditorPanel extends JPanel implements DebugEventListener {
    private static final String EMPTY_STR = "";
    private JEditorPane editor;
    private GDBDebugTarget debugTarget;
    private String debuggerLineNumber = EMPTY_STR;

    public static void main(String[] args) {
        JFrame frame = new JFrame("LineNumbersDemo");
        frame.getContentPane().add(new EditorPanel());
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public EditorPanel() {
        super(new BorderLayout());
        Boris.getDebugEventMgr().addListener(this);
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

    private String getRandomText() throws IOException {
        Random random = new Random();
        return Files.readAllLines(Paths.get("/usr/share/dict/words")).stream()
                .limit(300)
                .map(word -> (random.nextInt(15) == 0 ? "\n" : EMPTY_STR) + word)
                .collect(Collectors.joining(" "));
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
            thread.ifPresentOrElse(this::setDebuggerLineNumber, () -> debuggerLineNumber = EMPTY_STR);
            highlightCurrentInstructionLine();
            SwingUtilities.invokeLater(this::repaint);
            // TODO scroll to breakpoint line
        }
        else if (event.getType() == DebugEvent.EXITED
                || event.getType() == DebugEvent.TERMINATED) {
            debuggerLineNumber = EMPTY_STR;
            highlightCurrentInstructionLine();
            SwingUtilities.invokeLater(this::repaint);
        }
    }

    private void setDebuggerLineNumber(DSPThread thread) {
        StackFrame stackFrame = thread.getTopStackFrame();
        debuggerLineNumber = stackFrame != null ? String.valueOf(stackFrame.getLineNumber()) : EMPTY_STR;
    }

    private void highlightCurrentInstructionLine() {
        removeExistingHighlights();

        Position startPosition = editor.getDocument().getStartPosition();
        Position endPostion = editor.getDocument().getEndPosition();
        int startOffset = startPosition.getOffset();
        int endOffset = endPostion.getOffset();
        while (startOffset < endOffset) {
            try {
                String lineNumber = getLineNumber(startOffset);
                if (lineNumber != null && isCurrentDebuggerLine(lineNumber)) {
                    int rowStart = Utilities.getRowStart(editor, startOffset);
                    int rowEnd = Utilities.getRowEnd(editor, startOffset);
                    String lineText = editor.getDocument().getText(startOffset, rowEnd - rowStart);
                    if (!Strings.isNullOrEmpty(lineText)) {
                        Highlighter highlighter = editor.getHighlighter();
                        CurrentInstructionPointerHighlighter currentInstructionPointerHighlighter = new CurrentInstructionPointerHighlighter(Color.GREEN);
                        highlighter.addHighlight(rowStart, rowEnd, currentInstructionPointerHighlighter);
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

    private int getOffsetY(int offset) throws BadLocationException {
        FontMetrics fontMetrics = editor.getFontMetrics(editor.getFont());
        int descent = fontMetrics.getDescent();
        Rectangle r = editor.modelToView(offset);
        int y = r.y + r.height - descent;
        return y;
    }

    private String getLineNumber(int offset) {
        Element root = editor.getDocument().getDefaultRootElement();
        int index = root.getElementIndex(offset);
        Element line = root.getElement(index);
        return line.getStartOffset() == offset ? String.format("%3d", index + 1) : null;
    }

    private boolean isCurrentDebuggerLine(String lineNumber) {
        return debuggerLineNumber.equals(lineNumber.trim());
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

        private String getLineNumber(int offset) {
            Element root = editor.getDocument().getDefaultRootElement();
            int index = root.getElementIndex(offset);
            Element line = root.getElement(index);
            return line.getStartOffset() == offset ? String.format("%3d", index + 1) : null;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            Rectangle clip = g.getClipBounds();
            int startOffset = editor.viewToModel(new Point(0, clip.y));
            int endOffset = editor.viewToModel(new Point((0), clip.y + clip.height));
            while (startOffset <= endOffset) {
                try {
                    String lineNumber = getLineNumber(startOffset);
                    if (lineNumber != null) {
                        int x = getInsets().left + 2;
                        int y = getOffsetY(startOffset);
                        font = font != null ? font : new Font(Font.MONOSPACED, Font.BOLD, editor.getFont().getSize());
                        g.setFont(font);
//                        g.setColor(isCurrentDebuggerLine(lineNumber) ? Color.BLUE : Color.BLACK);
                        // TODO paint breakpoint lines RED
                        g.setColor(Color.BLACK);
                        g.drawString(lineNumber, x, y);
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

    private class CurrentInstructionPointerHighlighter extends  DefaultHighlighter.DefaultHighlightPainter {

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