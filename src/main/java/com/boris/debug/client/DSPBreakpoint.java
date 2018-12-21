package com.boris.debug.client;

import com.boris.debug.main.model.Breakpoint;

import java.nio.file.Path;

public class DSPBreakpoint implements Breakpoint {
    private boolean isEnabled = false;
    private Long lineNumber = null;
    private Path path = null;

    public DSPBreakpoint(Path path, Long lineNumber, boolean isEnabled) {
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
