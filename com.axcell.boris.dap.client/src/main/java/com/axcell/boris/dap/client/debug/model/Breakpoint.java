package com.axcell.boris.dap.client.debug.model;

import java.nio.file.Path;

public interface Breakpoint {
    boolean isEnabled();

    void setEnabled(boolean isEnabled);

    Path getPath();

    void setPath(Path path);

    Long getLineNumber();

    void setLineNumber(Long lineNumber);
}
