package com.boris.debug.client;

import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * BreakpointMgr manages the breakpoints that are currently set.
 */
public class BreakpointMgr {
    private Map<Source, List<SourceBreakpoint>> targetBreakpoints = new HashMap<>();
    private IDebugProtocolServer debugProtocolServer;

    public BreakpointMgr(IDebugProtocolServer debugProtocolServer) {
        this.debugProtocolServer = debugProtocolServer;
    }

    public void addBreakpoint(Source source, SourceBreakpoint sourceBreakpoint) {
        List<SourceBreakpoint> sourceBreakpoints = targetBreakpoints.computeIfAbsent(source, s -> new ArrayList<>());
        sourceBreakpoints.add(sourceBreakpoint);
        sendBreakpoints();
    }

    public void removeBreakpoint(Source source, SourceBreakpoint sourceBreakpoint) {
        if (targetBreakpoints.containsKey(source)) {
            targetBreakpoints.get(source).remove(sourceBreakpoint);
        }
        sendBreakpoints();
    }

    private CompletableFuture<Void> sendBreakpoints() {
        List<CompletableFuture<SetBreakpointsResponse>> all = new ArrayList<>();
        for (Map.Entry<Source, List<SourceBreakpoint>> entry : targetBreakpoints.entrySet()) {
            Source source = entry.getKey();
            List<SourceBreakpoint> breakpoints = entry.getValue();
            Long[] lines = breakpoints.stream()
                    .map(sourceBreakpoint -> sourceBreakpoint.getLine())
                    .toArray(Long[]::new);
            SourceBreakpoint[] sourceBreakpoints = breakpoints.toArray(new SourceBreakpoint[breakpoints.size()]);

            SetBreakpointsArguments arguments = new SetBreakpointsArguments();
            arguments.setSource(source);
            arguments.setLines(lines);
            arguments.setBreakpoints(sourceBreakpoints);
            arguments.setSourceModified(false);
            CompletableFuture<SetBreakpointsResponse> future = debugProtocolServer.setBreakpoints(arguments);
            all.add(future);
        }
        return CompletableFuture.allOf(all.toArray(new CompletableFuture[all.size()]));
    }
}
