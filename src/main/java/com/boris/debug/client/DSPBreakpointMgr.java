package com.boris.debug.client;

import com.boris.debug.main.model.IBreakpoint;
import com.boris.debug.main.model.IBreakpointListener;
import com.boris.debug.main.model.IBreakpointMgr;
import org.eclipse.lsp4j.debug.SetBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetBreakpointsResponse;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.SourceBreakpoint;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * DSPBreakpointMgr manages the breakpoints that are currently set.
 */
public class DSPBreakpointMgr implements IBreakpointListener {
    private Map<Source, List<SourceBreakpoint>> breakpoints = new HashMap<>();
    private IDebugProtocolServer debugProtocolServer;
    private IBreakpointMgr breakpointMgr;

    public DSPBreakpointMgr(IBreakpointMgr breakpointMgr, IDebugProtocolServer debugProtocolServer) {
        this.breakpointMgr = breakpointMgr;
        this.debugProtocolServer = debugProtocolServer;
    }

    public CompletableFuture<Void> initialize() {
        breakpointMgr.addBreakpointListener(this);
        // TODO send breakpoints to server
        return CompletableFuture.completedFuture(null);
    }

    private void addBreakpointToMap(IBreakpoint breakpoint) {
        // TODO
    }

    private void removeBreakpointFromMap(IBreakpoint breakpoint) {
        // TODO
    }

    public void addBreakpoint(Source source, SourceBreakpoint sourceBreakpoint) {
        List<SourceBreakpoint> sourceBreakpoints = breakpoints.computeIfAbsent(source, s -> new ArrayList<>());
        sourceBreakpoints.add(sourceBreakpoint);
        sendBreakpoints();
    }

    public void removeBreakpoint(Source source, SourceBreakpoint sourceBreakpoint) {
        if (breakpoints.containsKey(source)) {
            breakpoints.get(source).remove(sourceBreakpoint);
        }
        sendBreakpoints();
    }

    public CompletableFuture<Void> resendAllBreakpoints() {
        IBreakpoint[] breakpoints = breakpointMgr.getBreakpoints();
        for (IBreakpoint breakpoint : breakpoints) {
            if (breakpoint.isEnabled())
                addBreakpointToMap(breakpoint);
            else
                removeBreakpointFromMap(breakpoint);
        }
        return sendBreakpoints();
    }

    private CompletableFuture<Void> sendBreakpoints() {
        List<CompletableFuture<SetBreakpointsResponse>> all = new ArrayList<>();
        for (Map.Entry<Source, List<SourceBreakpoint>> entry : breakpoints.entrySet()) {
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

    @Override
    public void breakpointAdded(IBreakpoint breakpoint) {
        if (breakpoint.isEnabled()) {
            addBreakpointToMap(breakpoint);
            sendBreakpoints();
        }
    }

    @Override
    public void breakpointChanged(IBreakpoint breakpoint) {
        // TODO
    }

    @Override
    public void breakpointRemoved(IBreakpoint breakpoint) {
        removeBreakpointFromMap(breakpoint);
        sendBreakpoints();
    }
}
