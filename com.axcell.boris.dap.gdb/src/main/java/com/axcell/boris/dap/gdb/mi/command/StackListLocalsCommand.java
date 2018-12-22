package com.axcell.boris.dap.gdb.mi.command;

import java.util.Arrays;

public class StackListLocalsCommand extends Command {
    public StackListLocalsCommand() {
        super("-stack-list-locals", Arrays.asList(new String[]{"1"}));
    }
}
