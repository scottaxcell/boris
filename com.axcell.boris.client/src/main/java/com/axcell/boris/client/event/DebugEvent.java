package com.axcell.boris.client.event;

public class DebugEvent {
    public static final int STOPPED = 0;
    public static final int CONTINUED = 1;
    public static final int THREAD = 2;
    public static final int CONSOLE_OUTPUT = 3;
    public static final int TARGET_OUTPUT = 4;

    private int type;
    private Object source;
    private Object object;
    private boolean after;

    public DebugEvent(int type, Object source) {
        this.type = type;
        this.source = source;
        this.after = true;
    }

    public DebugEvent(int type, Object source, Object object) {
        this.type = type;
        this.source = source;
        this.object = object;
        this.after = true;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Object getSource() {
        return source;
    }

    public void setSource(Object source) {
        this.source = source;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public boolean isAfter() {
        return after;
    }

    public void setAfter(boolean after) {
        this.after = after;
    }
}
