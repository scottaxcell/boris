package com.boris.debug.server.mi.event;

import com.boris.debug.server.mi.output.Result;
import org.eclipse.lsp4j.debug.ExitedEventArguments;

public class ExitedEvent implements Event {
    private ExitedEventArguments args;

    public ExitedEventArguments getArgs() {
        return args;
    }

    private void setArgs(ExitedEventArguments args) {
        this.args = args;
    }

    public ExitedEvent parse(Result[] results) {
        // TODO sets event args
        return null;

    }
}
