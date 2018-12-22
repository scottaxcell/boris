package com.axcell.boris.client.debug.dsp;

import com.axcell.boris.client.debug.model.Variable;

public class DSPVariable extends DSPDebugElement implements Variable {
    private org.eclipse.lsp4j.debug.Variable variable;

    public DSPVariable(DSPDebugElement parent, org.eclipse.lsp4j.debug.Variable variable) {
        super(parent.getDebugClient());
        this.variable = variable;
    }

    @Override
    public String getValue() {
        return variable.getValue();
    }

    @Override
    public Variable[] getVariables() {
        return new Variable[0];
    }

    @Override
    public boolean hasVariables() {
        return false;
    }

    @Override
    public String getReferenceTypeName() {
        return null;
    }

    public String getName() {
        return variable.getName();
    }
}
