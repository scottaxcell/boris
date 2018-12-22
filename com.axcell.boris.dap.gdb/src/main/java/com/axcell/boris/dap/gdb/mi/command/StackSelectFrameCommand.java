package com.axcell.boris.dap.gdb.mi.command;

import java.util.Arrays;

public class StackSelectFrameCommand extends Command {
    public StackSelectFrameCommand(String frameId) {
        super("-stack-select-frame", Arrays.asList(new String[]{frameId}));
    }
}
