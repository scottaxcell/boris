package com.axcell.boris.dap.gdb.mi.command;

public class ExecNextCommand extends Command {
    // TODO add --reverse support
    public ExecNextCommand() {
        super("-exec-next");
    }
}
