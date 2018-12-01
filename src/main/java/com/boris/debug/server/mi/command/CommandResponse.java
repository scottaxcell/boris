package com.boris.debug.server.mi.command;

import com.boris.debug.server.mi.output.Output;

public class CommandResponse {
    private Output output;

    public CommandResponse(Output output) {
        this.output = output;
    }

    public Output getOutput() {
        return output;
    }
}
