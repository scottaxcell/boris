package com.boris.debug.main.ui;

import com.boris.debug.main.ui.event.IMyEventListener;
import com.boris.debug.main.ui.event.MyEvent;

import javax.swing.*;
import java.awt.*;

public class ConsolePanel extends JPanel implements IMyEventListener {
    JTextArea textArea;

    public ConsolePanel() {
        super(new BorderLayout());
        init();
    }

    private void init() {
        setBorder(BorderFactory.createTitledBorder("Console"));

        textArea = new JTextArea();
        textArea.setMinimumSize(new Dimension(300, 300));
        add(new JScrollPane(textArea));
    }


    @Override
    public void handleEvent(MyEvent event) {
        if (event.getType() == MyEvent.CONSOLE_OUTPUT) {
            if (event.getObject() instanceof String) {
                SwingUtilities.invokeLater(() -> {
                    textArea.append((String) event.getObject());
                    textArea.revalidate();
                });
            }
        }
    }
}
