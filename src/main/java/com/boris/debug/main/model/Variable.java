package com.boris.debug.main.model;

/**
 * Represents the value of a variable. A value representing a complex data structure contains variables
 */
public interface Variable {
    String getValueAsString();

    Variable[] getVariables();

    boolean hasVariables();

    String getReferenceTypeName();
}
