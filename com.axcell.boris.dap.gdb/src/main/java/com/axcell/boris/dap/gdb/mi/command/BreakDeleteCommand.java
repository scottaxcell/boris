package com.axcell.boris.dap.gdb.mi.command;

public class BreakDeleteCommand extends com.axcell.boris.dap.gdb.mi.command.Command {
    // TODO add support for deleting single breakpoints
    public BreakDeleteCommand() {
        super("-break-delete");
    }
}
