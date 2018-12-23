package com.axcell.boris.client.debug.dsp;

import com.axcell.boris.client.debug.model.DebugElement;

public class DSPDebugElement implements DebugElement {
    private GdbDebugTarget debugTarget;

    public DSPDebugElement(GdbDebugTarget debugTarget) {
        this.debugTarget = debugTarget;
    }

    @Override
    public GdbDebugTarget getDebugTarget() {
        return debugTarget;
    }
}
