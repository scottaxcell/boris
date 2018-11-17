package com.boris.debug.server;

import com.boris.debug.Utils;
import com.boris.debug.server.commands.MIBreakInsert;
import com.boris.debug.server.commands.MICommand;
import com.boris.debug.server.commands.MICommandFactory;
import com.boris.debug.server.commands.MIGdbExit;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;

import java.io.*;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;

public class GdbDebugProtocolServer implements IDebugProtocolServer {

    /*
    * need to track active gdb session
    * need to listen for gdb session "events" and do the right thing
    *
     */

    private Process gdbProcess;
    private GdbReaderThread gdbReaderThread;
    private GdbWriterThread gdbWriterThread;
    private final BlockingQueue<MICommand> writeCommands = new LinkedBlockingQueue<MICommand>();
    private final MICommandFactory miCommandFactory = new MICommandFactory();


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

        String[] cmdline = {Utils.GDB_PATH, "-q", "-nw", "-i", "mi2"};

        if (!args.isEmpty()) {
            // TODO this just a temporary hack for testing purposes
            List<String> tmp = new ArrayList<>();
            tmp.addAll(Arrays.asList(cmdline));
            for (Map.Entry<String, Object> entry : args.entrySet()) {
                tmp.add(entry.getKey());
            }
            cmdline = tmp.toArray(new String[0]);
        }
        Utils.debug(this.getClass().getSimpleName() + " -- launch called");

        try {
            Utils.debug(this.getClass().getSimpleName() + " -- executing: " + Arrays.toString(cmdline));
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
        if (args == null) {
            MIGdbExit miGdbExit = miCommandFactory.createGdbExit();
            writeCommands.add(miGdbExit);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments args) {
        Source source = args.getSource();
        String path = source.getPath();
        for (SourceBreakpoint breakpoint : args.getBreakpoints()) {
            Long line = breakpoint.getLine();
            StringBuilder stringBuilder = new StringBuilder(path);
            stringBuilder.append(':').append(line);
            MIBreakInsert breakInsert = miCommandFactory.createBreakInsert(stringBuilder.toString());
            writeCommands.add(breakInsert);
        }
        // TODO send response when response is returned from gdb
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

    private class GdbWriterThread extends Thread {
        private OutputStream outputStream;

        GdbWriterThread(OutputStream outputStream) {
            super("GDB Writer Thread");
            this.outputStream = outputStream;
        }

        @Override
        public void run() {
            MICommand command;

            while (true) {
                try {
                    command = writeCommands.take();
                } catch (InterruptedException e) {
                    break; // shutting down
                }

                String commandStr = command.constructCommand();
                Utils.debug(this.getClass().getSimpleName() + " -- writing: " + commandStr);
                try {
                    outputStream.write(commandStr.getBytes());
                    outputStream.flush();
                } catch (IOException e) {
                    break; // shutdown thread in case of IO error
                }
            }
            try {
                if (outputStream != null)
                    outputStream.close();
            } catch (IOException e) {
            }
        }
    }

    private class GdbReaderThread extends Thread {
        private InputStream inputStream;

        GdbReaderThread(InputStream inputStream) {
            super("GDB Reader Thread");
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.length() != 0) {
                        processMIOutput(line);
                    }
                }
            } catch (IOException e) {
                // socket is shut down
            } catch (RejectedExecutionException e) {
                // dispatch thread is down
            }
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
            }
        }

        private void processMIOutput(String line) {
            // TODO
            Utils.debug(this.getClass().getSimpleName() + " -- reading: " + line);
        }
    }
}
