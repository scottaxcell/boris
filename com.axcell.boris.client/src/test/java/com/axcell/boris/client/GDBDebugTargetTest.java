package com.axcell.boris.client;

import com.axcell.boris.client.debug.dsp.GDBDebugTarget;
import com.axcell.boris.client.debug.model.GlobalBreakpointMgr;
import com.axcell.boris.dap.gdb.Target;
import org.eclipse.lsp4j.debug.Capabilities;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class GDBDebugTargetTest {
    private static final int THREE_SECONDS = 3000;
    private static final int TWO_SECONDS = 2000;
    private static final int ONE_SECOND = 1000;
    private static final int HALF_SECOND = 500;

    private static final String[] makeCmdLine = {"make", "-f", "makefile"};
    private final String TEST_CASE_DIR = this.getClass().getResource("/threadexample").getPath();
    private final String SOURCE_FILENAME = this.getClass().getResource("/threadexample.cpp").getPath();
    private final String TARGET_FILENAME = String.format("%s/threadexample", TEST_CASE_DIR);
    private Target target = new Target(TARGET_FILENAME);

    private GlobalBreakpointMgr globalBreakpointMgr = GlobalBreakpointMgr.getInstance();

    private void compileThreadExampleExe() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(makeCmdLine);
            processBuilder.directory(new File(TEST_CASE_DIR));
            processBuilder.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @org.junit.Before
    public void setUp() throws Exception {
        compileThreadExampleExe();
    }

    @org.junit.After
    public void tearDown() throws InterruptedException {
    }

    @Test
    public void initialize() throws ExecutionException, InterruptedException {
        GDBDebugTarget client = new GDBDebugTarget(target, globalBreakpointMgr);
        client.initialize(42);
        Assert.assertTrue(client.isInitialized());

        Capabilities capabilities = new Capabilities();
        capabilities.setSupportsFunctionBreakpoints(false);
        capabilities.setSupportsConditionalBreakpoints(false);

        Assert.assertEquals(capabilities.toString(), client.getCapabilities().toString());
    }
}
