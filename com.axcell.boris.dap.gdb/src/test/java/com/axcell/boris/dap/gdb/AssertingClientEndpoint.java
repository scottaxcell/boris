package com.axcell.boris.dap.gdb;

import org.eclipse.lsp4j.debug.ExitedEventArguments;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.xtext.xbase.lib.Pair;
import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;
import org.junit.Assert;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AssertingClientEndpoint implements Endpoint, IDebugProtocolClient {
    public Map<String, Pair<Object, Object>> expectedRequests = new LinkedHashMap<>();
    public Map<String, Object> expectedNotifications = new LinkedHashMap<>();
    private boolean initializedExercised = false;
    private long exitReturnCode = Long.MAX_VALUE;
    private boolean stopped = false;
    private StoppedEventArguments stoppedEventArguments;

    @Override
    public CompletableFuture<?> request(String method, Object parameter) {
        Assert.assertTrue(expectedRequests.containsKey(method));
        Pair<Object, Object> result = expectedRequests.remove(method);
        Assert.assertEquals(result.getKey().toString(), parameter.toString());
        return CompletableFuture.completedFuture(result.getValue());
    }

    @Override
    public void notify(String method, Object parameter) {
        Assert.assertTrue(expectedNotifications.containsKey(method));
        Object object = expectedNotifications.remove(method);
        Assert.assertEquals(object.toString(), parameter.toString());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).addAllFields().toString();
    }

    /**
     * wait 1 second for request and notification expectations to be cleared out
     */
    public void joinOnEmpty() {
        long before = System.currentTimeMillis();
        do {
            if (expectedRequests.isEmpty() && expectedNotifications.isEmpty())
                return;
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        while (System.currentTimeMillis() < before + 1000);

        Assert.fail("expectations were not cleared out " + toString());
    }

    boolean isInitializedExercised() {
        return initializedExercised;
    }

    @Override
    public void initialized() {
        initializedExercised = true;
    }

    long exitedCleanly() {
        return exitReturnCode;
    }

    @Override
    public void exited(ExitedEventArguments args) {
        exitReturnCode = args.getExitCode();
    }

    boolean isStopped() {
        return stopped;
    }

    StoppedEventArguments getStoppedEventArguments() {
        return stoppedEventArguments;
    }

    @Override
    public void stopped(StoppedEventArguments args) {
        stopped = true;
        stoppedEventArguments = args;
    }
}

