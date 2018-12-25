package com.axcell.boris.dap.client.ui.event;

public class GUIEvent {
    public static final int THREAD_SELECTED = 0;
    public static final int STACK_FRAME_SELECTED = 1;
    public static final int BREAKPOINT_REMOVED = 2;

    private int type;
    private Object source;
    private Object object;
    private boolean after;

    public GUIEvent(int type, Object source) {
        this.type = type;
        this.source = source;
        this.after = true;
    }

    public GUIEvent(int type, Object source, Object object) {
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
