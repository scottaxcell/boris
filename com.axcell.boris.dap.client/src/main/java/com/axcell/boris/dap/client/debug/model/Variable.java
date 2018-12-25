package com.axcell.boris.dap.client.debug.model;

/**
 * Represents the value of a variable. A value representing a complex data structure contains variables
 */
public interface Variable {
    String getName();

    String getReferenceTypeName();

    Value getValue();
}
