package com.axcell.boris.dap.client.debug.dsp;

import com.axcell.boris.dap.client.debug.model.Variable;

public class DSPVariable extends DSPDebugElement implements Variable {
    private Long variablesReference;
    private String name;
    private String value;

    public DSPVariable(DSPDebugElement parent, Long variablesReference, String name, String value) {
        super(parent.getDebugTarget());
        this.variablesReference = variablesReference;
        this.name = name;
        this.value = value;
    }

    @Override
    public String getReferenceTypeName() {
        return null;
    }

    @Override
    public DSPValue getValue() {
        return new DSPValue(this, variablesReference, name, value);
    }

    public String getName() {
        return name;
    }
}
