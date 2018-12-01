package com.boris.debug.server.mi.command;

public class ExecContinueCommand extends Command {
    // TODO add --thread-group or --all support
    public ExecContinueCommand() {
        super("-exec-continue");
    }
}
