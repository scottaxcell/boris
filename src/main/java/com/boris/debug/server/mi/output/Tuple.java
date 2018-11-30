package com.boris.debug.server.mi.output;

/**
 * tuple -> "{}" | "{" result ( "," result )* "}"
 */
public class Tuple extends Value {
    private Value[] values;

    public Value[] getValues() {
        return values;
    }

    public void setValues(Value[] values) {
        this.values = values;
    }
}
