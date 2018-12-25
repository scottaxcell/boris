package com.axcell.boris.dap.client.debug.model;

public interface Value {
    String getReferenceTypeName();

    String getValueString();

    Variable[] getVariables();

    boolean hasVariables();
}
