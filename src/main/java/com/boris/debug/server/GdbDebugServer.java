package com.boris.debug.server;

import com.boris.debug.server.mi.command.*;
import com.boris.debug.server.mi.output.MIConst;
import com.boris.debug.server.mi.output.Output;
import com.boris.debug.server.mi.output.Result;
import com.boris.debug.server.mi.output.Tuple;
import com.boris.debug.server.mi.parser.Parser;
import com.boris.debug.server.mi.record.OutOfBandRecord;
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
    private final CommandFactory commandFactory = new CommandFactory();
    private IDebugProtocolClient client;
    private ExecutorService executor = Executors.newCachedThreadPool();
    private EventProcessor eventProcessor = new EventProcessor();
    private boolean gdbInitialized = false;
    /**
     * Commands that need to be processed
     */
    private final BlockingQueue<CommandWrapper> commandQueue = new LinkedBlockingQueue<>();
    /**
     * Commands that have been written to the GDB stream
     */
    private final List<CommandWrapper> writtenCommands = new ArrayList<>();
    /**
     * Commands that have been read from the GDB stream
     */
    private final Map<Integer, CommandWrapper> readCommands = Collections.synchronizedMap(new HashMap<Integer, CommandWrapper>());
    /**
     * Aligns commandWrapper requests with commandWrapper responses
     */
    private int tokenCounter = 0;

    public void setRemoteProxy(IDebugProtocolClient client) {
        this.client = client;
        eventProcessor.setClient(client);
    }

    private int getNewToken() {
        int newTokenCounter = ++tokenCounter;
        if (newTokenCounter <= 0)
            newTokenCounter = tokenCounter = 1;
        return newTokenCounter;
    }

    private int queueCommand(Command command) {
        int token = -1;
        final CommandWrapper commandWrapper = new CommandWrapper(command);
        commandQueue.add(commandWrapper);
        if (commandWrapper.getCommand().isRequiresResponse()) {
            commandWrapper.generateToken();
            token = commandWrapper.getToken();
        }
        Utils.debug("queued.. " + commandWrapper.getCommand().constructCommand());
        return token;
    }

    private int processNextQueuedCommand() {
//        int token = -1;
//        if (!commandQueue.isEmpty()) {
//            final CommandWrapper commandWrapper = commandQueue.remove(0);
//            if (commandWrapper != null) {
//                if (commandWrapper.getCommand().isRequiresResponse()) {
//                    commandWrapper.generateToken();
//                    token = commandWrapper.getToken();
//                }
////                writtenCommands.add(commandWrapper);
//                Utils.debug(commandWrapper.getCommand().constructCommand() + " processed..");
//            }
//        }
//        return token;
        return -1;
    }

    @Override
    public CompletableFuture<RunInTerminalResponse> runInTerminal(RunInTerminalRequestArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {
        Utils.debug("initialize");

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

        try {
            Utils.debug("starting GDB -- " + Arrays.toString(cmdline));
            gdbProcess = Runtime.getRuntime().exec(cmdline);

            InputStream inputStream = gdbProcess.getInputStream();
            OutputStream outputStream = gdbProcess.getOutputStream();

            gdbReaderThread = new GdbReaderThread(inputStream);
            gdbReaderThread.start();

            gdbWriterThread = new GdbWriterThread(outputStream);
            gdbWriterThread.start();
        }
        catch (IOException e) {
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
            GdbExitCommand miGdbExit = commandFactory.createGdbExit();
            queueCommand(miGdbExit);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments args) {
        List<Integer> tokens = new ArrayList<>();
        Source source = args.getSource();
        String path = source.getPath();
        for (SourceBreakpoint breakpoint : args.getBreakpoints()) {
            Long line = breakpoint.getLine();
            StringBuilder stringBuilder = new StringBuilder(path);
            stringBuilder.append(':').append(line);
            BreakInsertCommand breakInsertCommand = commandFactory.createBreakInsert(stringBuilder.toString());
            final int token = queueCommand(breakInsertCommand);
            tokens.add(token);
        }

        Supplier<SetBreakpointsResponse> supplier = setBreakpointsResponseSupplier(tokens, args);
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    private Supplier<SetBreakpointsResponse> setBreakpointsResponseSupplier(List<Integer> tokens, SetBreakpointsArguments args) {
        return new Supplier<SetBreakpointsResponse>() {
            @Override
            public SetBreakpointsResponse get() {
                // TODO start timer to flag any commandWrapper that doesn't get a response
                List<CommandWrapper> commandResponses = new ArrayList<>();
                while (commandResponses.size() != tokens.size()) {
                    for (Integer token : tokens) {
                        if (readCommands.containsKey(token)) {
                            CommandWrapper commandWrapper = readCommands.remove(token);
                            commandResponses.add(commandWrapper);
                        }
                    }
                    try {
                        Thread.sleep(200);
                    }
                    catch (InterruptedException ignored) {
                    }
                }
                return getSetBreakpointsResponse(commandResponses, args);
            }
        };
    }

    private SetBreakpointsResponse getSetBreakpointsResponse(List<CommandWrapper> commandWrappers, SetBreakpointsArguments args) {
        SetBreakpointsResponse response = new SetBreakpointsResponse();
        List<Breakpoint> breakpoints = new ArrayList<>();
        for (CommandWrapper commandWrapper : commandWrappers) {
            CommandResponse commandResponse = commandWrapper.getCommandResponse();
            Output output = commandResponse.getOutput();
            ResultRecord resultRecord = output.getResultRecord();

            if (resultRecord.getResultClass() == ResultRecord.ResultClass.DONE) {
                Result[] results = resultRecord.getResults();
                for (Result result : results) {
                    if ("bkpt".equals(result.getVariable())) {
                        Tuple tuple = (Tuple) result.getValue();
                        MIConst file = (MIConst) tuple.getFieldValue("file");
                        MIConst fullname = (MIConst) tuple.getFieldValue("fullname");
                        MIConst line = (MIConst) tuple.getFieldValue("line");
                        Source source = new Source();
                        source.setName(file.getcString());
                        source.setPath(fullname.getcString());
                        Breakpoint breakpoint = new Breakpoint();
                        breakpoint.setSource(source);
                        breakpoint.setLine(Long.parseLong(line.getcString()));
                        breakpoints.add(breakpoint);
                    }
                }
            }
            Utils.debug(this.getClass().getSimpleName() + " " + commandWrapper.getToken() + "." + commandWrapper.getCommand().constructCommand());
        }
        // TODO parse commandWrapper response into set breakpoint reponse maintaining same order as original args
        if (!breakpoints.isEmpty())
            response.setBreakpoints(breakpoints.toArray(new Breakpoint[breakpoints.size()]));
        return response;
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
        ExecContinueCommand execContinue = commandFactory.createExecContinue();
        queueCommand(execContinue);
        Supplier<ContinueResponse> supplier = continueResponseSupplier(args);
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    private Supplier<ContinueResponse> continueResponseSupplier(ContinueArguments args) {
        return new Supplier<ContinueResponse>() {
            @Override
            public ContinueResponse get() {
                ContinueResponse response = new ContinueResponse();
                return response;
            }
        };
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
                    commandWrapper = commandQueue.take();
                }
                catch (InterruptedException e) {
                    break;
                }

                if (commandWrapper.getCommand().isRequiresResponse())
                    writtenCommands.add(commandWrapper);

                StringBuilder commandBuilder = new StringBuilder();
                if (commandWrapper.getCommand().isRequiresResponse())
                    commandBuilder.append(commandWrapper.getToken());
                commandBuilder.append(commandWrapper.getCommand().constructCommand());

                try {
                    outputStream.write(commandBuilder.toString().getBytes());
                    outputStream.flush();
                    Utils.debug(commandBuilder.toString() + " written..");
                }
                catch (IOException e) {
                    break;
                }
            }
            try {
                if (outputStream != null)
                    outputStream.close();
            }
            catch (IOException ignored) {
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
            parser = new Parser();
        }

        @Override
        public void run() {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.length() != 0) {
                        Utils.debug("reading line: " + line);
                        handleOutput(line);
                    }
                }
                Utils.debug("GdbReaderThread while FINISHED");
            }
            catch (IOException | RejectedExecutionException ignored) {
                ignored.printStackTrace();
            }
            try {
                if (inputStream != null)
                    inputStream.close();
            }
            catch (IOException ignored) {
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

                    CommandResponse commandResponse = new CommandResponse(output);
                    commandWrapper.setCommandResponse(commandResponse);

                    readCommands.put(token, commandWrapper);
                    Utils.debug("received " + commandWrapper.getCommand().constructCommand());
                }
                else {
                    // treat response as an event
                    Output output = new Output(resultRecord);
                    executor.execute(() -> {
                        processEvent(output);
                    });
                }
            }
            else if (recordType == Parser.RecordType.GdbPrompt) {
                if (!gdbInitialized) {
                    // run target
                    ExecRunCommand execRunCommand = commandFactory.createExecRun();
                    queueCommand(execRunCommand);
                    gdbInitialized = true;
                }
            }
            else if (recordType == Parser.RecordType.OutOfBand) {
                OutOfBandRecord outOfBandRecord = parser.parseOutOfBandRecord(line);
                Output output = new Output(outOfBandRecord);
                executor.execute(() -> {
                    processEvent(output);
                });
            }
        }
    }

    private void processEvent(Output output) {
        eventProcessor.eventReceived(output);
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
     * Wrapper for handling commandWrapper requests and responses
     */
    private class CommandWrapper {
        private Command command;
        private CommandResponse commandResponse;
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

        public CommandResponse getCommandResponse() {
            return commandResponse;
        }

        public void setCommandResponse(CommandResponse commandResponse) {
            this.commandResponse = commandResponse;
        }
    }

    /**
     * Timer for detecting written commands that don't receive a response
     */
    private class CommandResponseTimer extends TimerTask {
        private CommandWrapper commandWrapper;

        public CommandResponseTimer(CommandWrapper commandWrapper) {
            this.commandWrapper = commandWrapper;
        }

        @Override
        public void run() {
            CommandWrapper writtenCommand = getWrittenCommand(commandWrapper.getToken());
            if (writtenCommand != null) {
                String msg = this.getClass().getSimpleName() + " " + writtenCommand.getCommand().constructCommand() + " did not received a response in time";
                Logger.getInstance().warning(msg);
                throw new RuntimeException(msg);
            }
        }
    }
}
