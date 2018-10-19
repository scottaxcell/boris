package com.boris.debug.com.boris.debug;

import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class GDBDebugProtocolServer implements IDebugProtocolServer {
    @Override
    public CompletableFuture<RunInTerminalResponse> runInTerminal(RunInTerminalRequestArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<Void> configurationDone(ConfigurationDoneArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<Void> launch(Map<String, Object> args) {
        return null;
    }

    @Override
    public CompletableFuture<Void> attach(Map<String, Object> args) {
        return null;
    }

    @Override
    public CompletableFuture<Void> restart(RestartArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<Void> disconnect(DisconnectArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<Void> terminate(TerminateArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<SetFunctionBreakpointsResponse> setFunctionBreakpoints(SetFunctionBreakpointsArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<Void> setExceptionBreakpoints(SetExceptionBreakpointsArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<ContinueResponse> continue_(ContinueArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<Void> next(NextArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<Void> stepIn(StepInArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<Void> stepOut(StepOutArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<Void> stepBack(StepBackArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<Void> reverseContinue(ReverseContinueArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<Void> restartFrame(RestartFrameArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<Void> goto_(GotoArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<Void> pause(PauseArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<StackTraceResponse> stackTrace(StackTraceArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<ScopesResponse> scopes(ScopesArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<VariablesResponse> variables(VariablesArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<SetVariableResponse> setVariable(SetVariableArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<SourceResponse> source(SourceArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<ThreadsResponse> threads() {
        return null;
    }

    @Override
    public CompletableFuture<Void> terminateThreads(TerminateThreadsArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<ModulesResponse> modules(ModulesArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<LoadedSourcesResponse> loadedSources(LoadedSourcesArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<EvaluateResponse> evaluate(EvaluateArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<SetExpressionResponse> setExpression(SetExpressionArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<StepInTargetsResponse> stepInTargets(StepInTargetsArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<GotoTargetsResponse> gotoTargets(GotoTargetsArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<CompletionsResponse> completions(CompletionsArguments args) {
        return null;
    }

    @Override
    public CompletableFuture<ExceptionInfoResponse> exceptionInfo(ExceptionInfoArguments args) {
        return null;
    }
}
