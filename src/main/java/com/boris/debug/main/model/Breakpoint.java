package com.boris.debug.main.model;

import java.nio.file.Path;

public class Breakpoint implements IBreakpoint {
    boolean isEnabled = false;
    Long lineNumber = null;
    Path path = null;

    public Breakpoint(Path path, Long lineNumber, boolean isEnabled) {
        this.path = path;
        this.lineNumber = lineNumber;
        this.isEnabled = isEnabled;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public void setPath(Path path) {
        this.path = path;
    }

    @Override
    public Long getLineNumber() {
        return lineNumber;
    }

    @Override
    public void setLineNumber(Long lineNumber) {
        this.lineNumber = lineNumber;
    }
}
