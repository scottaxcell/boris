package com.boris.debug.server;

import com.boris.debug.Utils;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Thread;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class GdbDebugProtocolServer implements IDebugProtocolServer {

    /*
    * need to track active gdb session
    * need to listen for gdb session "events" and do the right thing
    *
     */

    private Process gdbProcess;
    private GdbReaderThread gdbReaderThread;
    private GdbWriterThread gdbWriterThread;

    @Override
    public CompletableFuture<RunInTerminalResponse> runInTerminal(RunInTerminalRequestArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {
        Utils.debug(this.getClass().getSimpleName() + " -- initialize called");

        Capabilities capabilities = new Capabilities();
        capabilities.setSupportsFunctionBreakpoints(false);
        capabilities.setSupportsConditionalBreakpoints(false);

        return CompletableFuture.completedFuture(capabilities);
    }

    @Override
    public CompletableFuture<Void> configurationDone(ConfigurationDoneArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> launch(Map<String, Object> args) {
        // args: target: exe to run
        // https://sourceware.org/gdb/onlinedocs/gdb/Mode-Options.html
        // cmd: gdb -q -nw -i mi2 [target]
        // -q: quiet, -nw: no windows i: interpreter (mi2 in our case)

        final String[] cmdline = {Utils.GDB_PATH, "-q", "-nw", "-i", "mi2"};

        Utils.debug(this.getClass().getSimpleName() + " -- launch called");

        try {
            gdbProcess = Runtime.getRuntime().exec(cmdline);
            InputStream inputStream = gdbProcess.getInputStream();
            OutputStream outputStream = gdbProcess.getOutputStream();

            gdbReaderThread = new GdbReaderThread(inputStream);
            gdbReaderThread.start();

            gdbWriterThread = new GdbWriterThread(outputStream);
            gdbWriterThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO sleep is just for test purposes to see if gdb is actually fired up and reader sees output
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> attach(Map<String, Object> args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> restart(RestartArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> disconnect(DisconnectArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> terminate(TerminateArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<SetFunctionBreakpointsResponse> setFunctionBreakpoints(SetFunctionBreakpointsArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> setExceptionBreakpoints(SetExceptionBreakpointsArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ContinueResponse> continue_(ContinueArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> next(NextArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stepIn(StepInArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stepOut(StepOutArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stepBack(StepBackArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> reverseContinue(ReverseContinueArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> restartFrame(RestartFrameArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> goto_(GotoArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> pause(PauseArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<StackTraceResponse> stackTrace(StackTraceArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ScopesResponse> scopes(ScopesArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<VariablesResponse> variables(VariablesArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<SetVariableResponse> setVariable(SetVariableArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<SourceResponse> source(SourceArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ThreadsResponse> threads() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> terminateThreads(TerminateThreadsArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ModulesResponse> modules(ModulesArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<LoadedSourcesResponse> loadedSources(LoadedSourcesArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<EvaluateResponse> evaluate(EvaluateArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<SetExpressionResponse> setExpression(SetExpressionArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<StepInTargetsResponse> stepInTargets(StepInTargetsArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<GotoTargetsResponse> gotoTargets(GotoTargetsArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<CompletionsResponse> completions(CompletionsArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ExceptionInfoResponse> exceptionInfo(ExceptionInfoArguments args) {
        return CompletableFuture.completedFuture(null);
    }
}
