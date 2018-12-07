package com.boris.debug.server;

import com.boris.debug.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GdbBackend {
    // TODO track state of affairs
    public enum State {NOT_INITIALIZED, STARTED, TERMINATED}

    private Process gdbProcess;
    private Target target;

    // cmd: gdb -q -nw -i mi2 target
    // -q: quiet, -nw: no windows i: interpreter (mi2 in our case)
    private static String[] baseCmdLine = {Utils.GDB_PATH, "-q", "-nw", "-i", "mi2"};

    public GdbBackend(Target target) {
        this.target = target;
    }

    public InputStream getInputStream() {
        return gdbProcess.getInputStream();
    }

    public OutputStream getOutputStream() {
        return gdbProcess.getOutputStream();
    }

    public InputStream getErrorStream() {
        return gdbProcess.getErrorStream();
    }

    public void startGdb() {
        gdbProcess = launchGdbProcess(getCmdLine());
    }

    private Process launchGdbProcess(String[] cmdLine) {
        Utils.debug("starting GDB -- " + Arrays.toString(cmdLine));
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(cmdLine);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return process;
    }

    private String[] getCmdLine() {
        List<String> cmdLine = new ArrayList<>();
        cmdLine.addAll(Arrays.asList(baseCmdLine));
        cmdLine.add(target.getPath().toString());
        return cmdLine.toArray(new String[cmdLine.size()]);
    }
}
