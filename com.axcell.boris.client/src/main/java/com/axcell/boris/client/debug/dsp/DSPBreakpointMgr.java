package com.axcell.boris.client.debug.dsp;

import com.axcell.boris.client.debug.model.Breakpoint;
import com.axcell.boris.client.debug.model.BreakpointListener;
import com.axcell.boris.client.debug.model.BreakpointMgr;
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
public class DSPBreakpointMgr implements BreakpointListener {
    private Map<Source, List<SourceBreakpoint>> breakpoints = new HashMap<>();
    private IDebugProtocolServer debugProtocolServer;
    private BreakpointMgr breakpointMgr;

    public DSPBreakpointMgr(BreakpointMgr breakpointMgr, IDebugProtocolServer debugProtocolServer) {
        this.breakpointMgr = breakpointMgr;
        this.debugProtocolServer = debugProtocolServer;
    }

    public CompletableFuture<Void> initialize() {
        breakpointMgr.addBreakpointListener(this);
        return resendAllBreakpoints();
    }

    private void addBreakpoint(Breakpoint breakpoint) {
        if (breakpoint instanceof DSPBreakpoint)
            addBreakpoint(createSource(breakpoint), createSourceBreakpoint(breakpoint));
    }

    private void removeBreakpoint(Breakpoint breakpoint) {
        if (breakpoint instanceof DSPBreakpoint)
            removeBreakpoint(createSource(breakpoint), createSourceBreakpoint(breakpoint));
    }

    private SourceBreakpoint createSourceBreakpoint(Breakpoint breakpoint) {
        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(breakpoint.getLineNumber());
        return sourceBreakpoint;
    }

    private Source createSource(Breakpoint breakpoint) {
        Source source = new Source();
        source.setName(String.valueOf(breakpoint.getPath().getFileName()));
        source.setPath(breakpoint.getPath().toString());
        return source;
    }

    private void addBreakpoint(Source source, SourceBreakpoint sourceBreakpoint) {
        List<SourceBreakpoint> sourceBreakpoints = breakpoints.computeIfAbsent(source, s -> new ArrayList<>());
        sourceBreakpoints.add(sourceBreakpoint);
    }

    private void removeBreakpoint(Source source, SourceBreakpoint sourceBreakpoint) {
        if (breakpoints.containsKey(source)) {
            breakpoints.get(source).remove(sourceBreakpoint);
        }
    }

    private CompletableFuture<Void> resendAllBreakpoints() {
        Breakpoint[] breakpoints = breakpointMgr.getBreakpoints();
        for (Breakpoint breakpoint : breakpoints) {
            if (breakpoint.isEnabled())
                addBreakpoint(breakpoint);
            else
                removeBreakpoint(breakpoint);
        }
        return sendBreakpoints();
    }

    private CompletableFuture<Void> sendBreakpoints() {
        List<CompletableFuture<SetBreakpointsResponse>> setBreakpointResponses = new ArrayList<>();
        for (Map.Entry<Source, List<SourceBreakpoint>> entry : breakpoints.entrySet()) {
            Source source = entry.getKey();
            List<SourceBreakpoint> breakpoints = entry.getValue();
            Long[] lines = breakpoints.stream()
                    .map(SourceBreakpoint::getLine)
                    .toArray(Long[]::new);
            SourceBreakpoint[] sourceBreakpoints = breakpoints.toArray(new SourceBreakpoint[breakpoints.size()]);

            SetBreakpointsArguments arguments = new SetBreakpointsArguments();
            arguments.setSource(source);
            arguments.setLines(lines);
            arguments.setBreakpoints(sourceBreakpoints);
            arguments.setSourceModified(false);
            CompletableFuture<SetBreakpointsResponse> future = debugProtocolServer.setBreakpoints(arguments);
            setBreakpointResponses.add(future);
        }
        return CompletableFuture.allOf(setBreakpointResponses.toArray(new CompletableFuture[setBreakpointResponses.size()]));
    }

    @Override
    public void breakpointAdded(Breakpoint breakpoint) {
        if (breakpoint.isEnabled()) {
            addBreakpoint(breakpoint);
        }
        sendBreakpoints();
    }

    @Override
    public void breakpointChanged(Breakpoint breakpoint) {
        if (breakpoint.isEnabled()) {
            addBreakpoint(breakpoint);
        }
        else {
            removeBreakpoint(breakpoint);
        }
        sendBreakpoints();
    }

    @Override
    public void breakpointRemoved(Breakpoint breakpoint) {
        removeBreakpoint(breakpoint);
        sendBreakpoints();
    }

    public void cleanup() {
        if (breakpointMgr != null)
            breakpointMgr.removeBreakpointListener(this);
    }
}
