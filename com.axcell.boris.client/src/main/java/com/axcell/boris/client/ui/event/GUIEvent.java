package com.axcell.boris.client.ui.event;

public class GUIEvent {

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
