package com.boris.debug.main.model;

import java.nio.file.Path;

public interface IBreakpoint {
    boolean isEnabled();

    void setEnabled(boolean isEnabled);

    Path getPath();

    void setPath(Path path);

    Long getLineNumber();

    void setLineNumber(Long lineNumber);
}
