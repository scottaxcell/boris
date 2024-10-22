package com.axcell.boris.dap.client.debug.dsp;

import com.axcell.boris.dap.client.debug.model.Breakpoint;

import java.nio.file.Path;
import java.util.Objects;

public class DSPBreakpoint implements Breakpoint {
    private boolean isEnabled;
    private Long lineNumber;
    private Path path;

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

    @Override
    public boolean equals(Object o) {
        if (o instanceof DSPBreakpoint) {
            DSPBreakpoint other = (DSPBreakpoint) o;
            return Objects.equals(lineNumber, other.getLineNumber()) && Objects.equals(path, other.getPath());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineNumber, path);
    }
}
