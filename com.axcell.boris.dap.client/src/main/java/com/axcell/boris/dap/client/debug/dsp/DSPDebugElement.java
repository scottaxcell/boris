package com.axcell.boris.dap.client.debug.dsp;

import com.axcell.boris.dap.client.debug.model.DebugElement;

public class DSPDebugElement implements DebugElement {
    private GDBDebugTarget debugTarget;

    public DSPDebugElement(GDBDebugTarget debugTarget) {
        this.debugTarget = debugTarget;
    }

    @Override
    public GDBDebugTarget getDebugTarget() {
        return debugTarget;
    }
}
