package com.boris.debug.main.ui;

import com.boris.debug.main.event.DebugEvent;
import com.boris.debug.main.event.DebugEventListener;

import javax.swing.*;
import java.awt.*;

public class ConsolePanel extends JPanel implements DebugEventListener {
    JTextArea textArea;

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


    @Override
    public void handleEvent(DebugEvent event) {
        if (event.getObject() instanceof String) {
            String prefix = null;
            if (event.getType() == DebugEvent.CONSOLE_OUTPUT) {
                prefix = "GDB DEBUG: ";
            }
            else if (event.getType() == DebugEvent.TARGET_OUTPUT) {
                prefix = "TARGET OUTPUT: ";
            }
            String finalPrefix = prefix;
            SwingUtilities.invokeLater(() -> {
                textArea.append(finalPrefix + (String) event.getObject());
                textArea.revalidate();
            });
        }
    }
}
