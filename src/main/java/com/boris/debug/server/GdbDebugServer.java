package com.boris.debug.server;

import com.boris.debug.server.mi.command.CommandFactory;
import com.boris.debug.server.mi.output.Output;
import com.boris.debug.server.mi.parser.Parser;
import com.boris.debug.server.mi.command.Command;
import com.boris.debug.server.mi.record.ResultRecord;
import com.boris.debug.utils.Logger;
import com.boris.debug.utils.Utils;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;

import java.io.*;
import java.lang.Thread;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * GDB Server - implements DAP interface
 */
public class GdbDebugServer implements IDebugProtocolServer {
    private Process gdbProcess;
    private GdbReaderThread gdbReaderThread;
    private GdbWriterThread gdbWriterThread;
    private final CommandFactory CommandFactory = new CommandFactory();
    private IDebugProtocolClient client;
    private ExecutorService executor = Executors.newCachedThreadPool();
    /**
     * Commands that need to be processed
     */
    private final List<CommandWrapper> commandQueue = new ArrayList<>();
    /**
     * Commands that have been written to the GDB stream
     */
    private final BlockingQueue<CommandWrapper> writtenCommands = new LinkedBlockingQueue<>();
    /**
     * Commands that have been read from the GDB stream
     */
    private final Map<Integer, CommandWrapper> readCommands = Collections.synchronizedMap(new HashMap<Integer, CommandWrapper>());
    /**
     * Aligns command requests with command responses
     */
    private int tokenCounter = 0;


    public void setRemoteProxy(IDebugProtocolClient client) {
        this.client = client;
    }

    private int getNewToken() {
        int newTokenCounter = ++tokenCounter;
        if (newTokenCounter <= 0)
            newTokenCounter = tokenCounter = 1;
        return newTokenCounter;
    }

//    public void queueCommand(MICommand miCommand) {
//        final CommandWrapper commandHandle = new CommandWrapper(miCommand);
//        commandQueue.add(commandHandle);
//        processNextQueuedCommand();
//    }

//    private void processNextQueuedCommand() {
//        if (!commandQueue.isEmpty()) {
//            final CommandWrapper commandHandle = commandQueue.remove(0);
//            // TODO handle RawCommand scenario
//            commandHandle.generateTokenId();
//            if (commandHandle != null) {
//                writtenCommands.add(commandHandle);
//            }
//        }
//    }

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
//        if (args == null) {
//            MIGdbExit miGdbExit = miCommandFactory.createGdbExit();
//            queueCommand(miGdbExit);
//        }
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
//            MIBreakInsert breakInsert = miCommandFactory.createBreakInsert(stringBuilder.toString());
//            queueCommand(breakInsert);
        }
        Supplier<SetBreakpointsResponse> supplier = new Supplier<SetBreakpointsResponse>() {
            @Override
            public SetBreakpointsResponse get() {
//                while (true) {
//                    if (readCommands.remove())
//                }
                return null;
            }
        };
        return CompletableFuture.supplyAsync(supplier, executor);
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
//        MIExecContinue miExecContinue = miCommandFactory.createExecContinue();
//        queueCommand(miExecContinue);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> next(NextArguments args) {
//        MIGdbNext miGdbNext = miCommandFactory.createGdbNext();
//        queueCommand(miGdbNext);
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

    /**
     * Handles MI commands that are written to the GDB stream
     */
    private class GdbWriterThread extends Thread {
        private OutputStream outputStream;

        GdbWriterThread(OutputStream outputStream) {
            super("GDB Writer Thread");
            this.outputStream = outputStream;
        }

        @Override
        public void run() {
            CommandWrapper commandWrapper;

            while (true) {
                try {
                    commandWrapper = writtenCommands.take();
                } catch (InterruptedException e) {
                    break;
                }

                // TODO handle case of non-reponse command
                readCommands.put(commandWrapper.getToken(), commandWrapper);

                String commandStr;
                commandStr = commandWrapper.getToken() + commandWrapper.getCommand().constructCommand();
                Logger.getInstance().fine(this.getClass().getSimpleName() + " -- writing: " + commandStr);
                try {
                    outputStream.write(commandStr.getBytes());
                    outputStream.flush();
                } catch (IOException e) {
                    break;
                }
            }
            try {
                if (outputStream != null)
                    outputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Handles MI output from the GDB stream
     */
    private class GdbReaderThread extends Thread {
        private InputStream inputStream;
        private Parser parser;

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
                        handleOutput(line);
                    }
                }
            } catch (IOException ignored) {
            } catch (RejectedExecutionException ignored) {
            }
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException ignored) {
            }
        }

        private void handleOutput(String line) {
            Parser.RecordType recordType = parser.getRecordType(line);

            if (recordType == Parser.RecordType.Result) {
                ResultRecord resultRecord = parser.parseResultRecord(line);
                int token = resultRecord.getToken();
                CommandWrapper commandWrapper = getWrittenCommand(token);
                if (commandWrapper != null) {
                    writtenCommands.remove(commandWrapper);
                    Output output = new Output(resultRecord);

                    CommandWrapper readCommandWrapper = null;
                    readCommands.put(token, readCommandWrapper);
                }
                else {
                    // TODO treat response as an event
                }
            }
            else if (recordType == Parser.RecordType.Stream) {
                // TODO
            }
            else if (recordType == Parser.RecordType.Async) {
                // TODO
            }
        }
    }

    private CommandWrapper getWrittenCommand(int token) {
        for (CommandWrapper commandWrapper : writtenCommands) {
            if (commandWrapper.getToken() == token) {
                return commandWrapper;
            }
        }
        return null;
    }

    /**
     * Wrapper for handling command requests and responses
     */
    private class CommandWrapper {
        private Command command;
        private int token;

        public CommandWrapper(Command command) {
            this.command = command;
            token = -1;
        }

        public Command getCommand() {
            return command;
        }

        public void setCommand(Command command) {
            this.command = command;
        }

        public int getToken() {
            return token;
        }

        public void setToken(int token) {
            this.token = token;
        }

        public void generateToken() {
            token = getNewToken();
        }
    }
}
