package com.boris.debug.main.ui.event;

public class MyEvent {
    public static final int DEBUGEE_STOPPED = 0;
    public static final int DEBUGEE_CONTINUED = 1;
    public static final int DEBUGEE_EXITED = 2;
    public static final int DEBUGEE_TERMINATED = 3;
    public static final int CONSOLE_OUTPUT = 4;

    private int type;
    private Object source;
    private Object object;
    private boolean after;

    public MyEvent(int type, Object source) {
        this.type = type;
        this.source = source;
        this.after = true;
    }

    public MyEvent(int type, Object source, Object object) {
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
