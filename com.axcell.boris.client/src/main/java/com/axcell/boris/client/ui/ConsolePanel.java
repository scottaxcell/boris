package com.axcell.boris.client.ui;

import com.axcell.boris.client.debug.event.DebugEvent;
import com.axcell.boris.client.debug.event.DebugEventListener;

import javax.swing.*;
import java.awt.*;

public class ConsolePanel extends JPanel implements DebugEventListener {
    private JTextArea textArea;

    public ConsolePanel() {
        super(new BorderLayout());
        Boris.getDebugEventMgr().addListener(this);
        init();
    }

    private void init() {
        setBorder(BorderFactory.createTitledBorder("Console"));

        textArea = new JTextArea();
        add(new JScrollPane(textArea));
    }

    private void cleanup() {
        Boris.getDebugEventMgr().removeListener(this);
    }

    @Override
    public void handleEvent(DebugEvent event) {
        if (event.getObject() instanceof String && event.getType() == DebugEvent.TARGET_OUTPUT)
            SwingUtilities.invokeLater(() -> appendString((String) event.getObject()));
    }

    private void appendString(String string) {
        textArea.append(string);
        textArea.revalidate();
    }
}
