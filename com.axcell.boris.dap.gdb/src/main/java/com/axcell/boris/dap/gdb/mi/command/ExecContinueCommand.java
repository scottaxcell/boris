package com.axcell.boris.dap.gdb.mi.command;

import java.util.Collections;

public class ExecContinueCommand extends Command {
    // TODO add --thread-group or --all support
    public ExecContinueCommand() {
        super("-exec-continue", Collections.singletonList("--all"));
    }
}