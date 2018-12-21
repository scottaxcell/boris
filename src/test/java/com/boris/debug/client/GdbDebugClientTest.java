package com.boris.debug.client;

import com.boris.debug.main.model.BreakpointMgr;
import com.boris.debug.server.Target;
import org.eclipse.lsp4j.debug.Capabilities;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class GdbDebugClientTest {
    private static final int THREE_SECONDS = 3000;
    private static final int TWO_SECONDS = 2000;
    private static final int ONE_SECOND = 1000;
    private static final int HALF_SECOND = 500;

    private static final String TEST_CASE_DIR = "/home/saxcell/dev/boris/testcases/helloworld";
    private static final String SOURCE_FILENAME = String.format("%s/helloworld.cpp", TEST_CASE_DIR);
    private static final String TARGET_FILENAME = String.format("%s/helloworld", TEST_CASE_DIR);
    private Target target = new Target(TARGET_FILENAME);
    private BreakpointMgr breakpointMgr = BreakpointMgr.getInstance();

    @org.junit.Before
    public void setUp() throws Exception {
    }

    @org.junit.After
    public void tearDown() throws InterruptedException {
    }

    @Test
    public void initialize() throws ExecutionException, InterruptedException {
        GdbDebugClient client = new GdbDebugClient(target, breakpointMgr);
        client.initialize(42);
        Assert.assertTrue(client.isInitialized());

        Capabilities capabilities = new Capabilities();
        capabilities.setSupportsFunctionBreakpoints(false);
        capabilities.setSupportsConditionalBreakpoints(false);

        Assert.assertEquals(capabilities.toString(), client.getCapabilities().toString());
    }
}
