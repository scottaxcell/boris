package com.boris.debug.client.ui;

import com.boris.debug.utils.Logger;
import com.boris.debug.utils.Utils;
import org.eclipse.lsp4j.debug.BreakpointEventArguments;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;

import javax.swing.*;
import java.awt.*;

public class BreakpointsPanel extends JPanel implements IDebugProtocolClient {
    // MIList element: file name and line number
    private JList list;
    private DefaultListModel model;

    public BreakpointsPanel() {
        super(new BorderLayout());
        init();
    }

    public void init() {
        model = new DefaultListModel();
        model.add(0, "bogus.cpp : 13");
        model.add(1,"main.cpp :16");
        model.add(2,"vadd.cpp :4");

        list = new JList(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.setSelectedIndex(0);
        JScrollPane listScrollPane = new JScrollPane(list);
        add(listScrollPane);
    }

    @Override
    public void stopped(StoppedEventArguments args) {
        Logger.getInstance().fine(this.getClass().getSimpleName() + ": stopped");
    }

    @Override
    public void breakpoint(BreakpointEventArguments args) {
        Logger.getInstance().fine(this.getClass().getSimpleName() + ": breakpoint");
    }
}
