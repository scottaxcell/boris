package com.boris.debug.client;

import com.boris.debug.main.model.IVariable;

public class DSPVariable extends DSPDebugElement implements IVariable {
    private String name;
    private String value;
    private Long variablesReference;

    public DSPVariable(DSPDebugElement parent, Long variablesReference, String name, String value) {
        super(parent.getDebugClient());
        this.variablesReference = variablesReference;
        this.name = name;
        this.value = value;
    }

    @Override
    public String getValueAsString() {
        return null;
    }

    @Override
    public IVariable[] getVariables() {
        return new IVariable[0];
    }

    @Override
    public boolean hasVariables() {
        return false;
    }

    @Override
    public String getReferenceTypeName() {
        return null;
    }
}
