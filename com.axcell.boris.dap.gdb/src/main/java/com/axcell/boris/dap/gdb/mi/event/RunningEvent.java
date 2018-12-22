package com.axcell.boris.dap.gdb.mi.event;

import org.eclipse.lsp4j.debug.ContinuedEventArguments;

public class RunningEvent extends Event {
    private ContinuedEventArguments args;

    public ContinuedEventArguments getArgs() {
        return args;
    }

}
