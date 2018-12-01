package com.boris.debug.server.mi.output;

/**
 * const -> c-string
 */
public class MIConst extends Value {
    private String cString;

    public String getcString() {
        return cString;
    }

    public void setcString(String cString) {
        this.cString = cString;
    }
}
