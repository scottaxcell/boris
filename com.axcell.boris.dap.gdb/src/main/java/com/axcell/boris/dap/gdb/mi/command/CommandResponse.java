package com.axcell.boris.dap.gdb.mi.command;

import com.axcell.boris.dap.gdb.mi.output.Output;

public class CommandResponse {
    private Output output;

    public CommandResponse(Output output) {
        this.output = output;
    }

    public Output getOutput() {
        return output;
    }
}
