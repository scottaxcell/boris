package com.axcell.boris.client.ui;

import javax.swing.*;
import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

public class EditorPanel extends JPanel {
    private JEditorPane editorPane;

    private static final String TEST_CASE_DIR = "/home/saxcell/dev/boris/testcases/helloworld";
    private static final String SOURCE_FILENAME = String.format("%s/helloworld.cpp", TEST_CASE_DIR);

    public EditorPanel() {
        super(new BorderLayout());
        init();
    }

    private void init() {
        setBorder(BorderFactory.createTitledBorder("Editor"));

        editorPane = new JEditorPane();
        editorPane.setEditable(false);

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
}
