package com.axcell.boris.client.debug.model;

/**
 * Represents the value of a variable. A value representing a complex data structure contains variables
 */
public interface Variable {
    String getValue();

    Variable[] getVariables();

    boolean hasVariables();

    String getReferenceTypeName();

    String getName();
}
