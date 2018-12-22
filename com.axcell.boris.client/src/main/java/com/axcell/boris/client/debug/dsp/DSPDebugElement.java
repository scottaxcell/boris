package com.axcell.boris.client.debug.dsp;

import com.axcell.boris.client.GdbDebugClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DSPDebugElement {
    private GdbDebugClient debugClient;

    public DSPDebugElement(GdbDebugClient client) {
        this.debugClient = client;
    }

    public DSPDebugElement() {
    }

    public GdbDebugClient getDebugClient() {
        return debugClient;
    }

    static <T> T complete(CompletableFuture<T> future) {
        try {
            return future.get();
        }
        catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("failed to get result from target", e);
        }
    }
}
