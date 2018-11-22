package com.boris.debug.server;

import com.boris.debug.utils.Utils;
import com.boris.debug.client.GdbDebugClient;
import com.boris.debug.server.commands.*;
import com.boris.debug.server.output.*;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;

import java.io.*;
import java.lang.Thread;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;

public class GdbDebugServer implements IDebugProtocolServer {

    /*
    * need to track active gdb session
    * need to listen for gdb session "events" and do the right thing
    *
     */

    private Process gdbProcess;
    private GdbReaderThread gdbReaderThread;
    private GdbWriterThread gdbWriterThread;
    private final List<CommandHandle> commandQueue = new ArrayList<CommandHandle>();
    private final BlockingQueue<CommandHandle> txCommands = new LinkedBlockingQueue<CommandHandle>();
    private final Map<Integer, CommandHandle> rxCommands = Collections.synchronizedMap(new HashMap<Integer, CommandHandle>());
    private final MICommandFactory miCommandFactory = new MICommandFactory();
    private int tokenIdCounter = 0;
    private GdbDebugClient client;

    private int getNewTokenId() {
        int count = ++tokenIdCounter;
        // in case we ever wrap around
        if (count <= 0) {
            count = tokenIdCounter = 1;
        }
        return count;
    }

    public void queueCommand(MICommand miCommand) {
        final CommandHandle commandHandle = new CommandHandle(miCommand);
        commandQueue.add(commandHandle);
        processNextQueuedCommand();
    }

    private void processNextQueuedCommand() {
        if (!commandQueue.isEmpty()) {
            final CommandHandle commandHandle = commandQueue.remove(0);
            // TODO handle RawCommand scenario
            commandHandle.generateTokenId();
            if (commandHandle != null) {
                txCommands.add(commandHandle);
            }
        }
    }

    @Override
    public CompletableFuture<RunInTerminalResponse> runInTerminal(RunInTerminalRequestArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Capabilities> initialize(GdbDebugClient client, InitializeRequestArguments args) {
        this.client = client;
        return initialize(args);
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
            queueCommand(miGdbExit);
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
            queueCommand(breakInsert);
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
        MIExecContinue miExecContinue = miCommandFactory.createExecContinue();
        queueCommand(miExecContinue);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> next(NextArguments args) {
        MIGdbNext miGdbNext = miCommandFactory.createGdbNext();
        queueCommand(miGdbNext);
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
            CommandHandle commandHandle;

            while (true) {
                try {
                    commandHandle = txCommands.take();
                } catch (InterruptedException e) {
                    break; // shutting down
                }

                // TODO implement RawCommand concept
//                if (!(commandHandle.getCommand() instanceof RawCommand)) {
                    // RawCommands will not get an answer, so there is no need to add them to receive queue
                    rxCommands.put(commandHandle.getTokenId(), commandHandle);
//                }

                String commandStr;
                // TODO handle RawCommand scenario
                commandStr = commandHandle.getTokenId() + commandHandle.getCommand().constructCommand();
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

    /**
     * Handles MI output from the GDB stream
     */
    private class GdbReaderThread extends Thread {
        private InputStream inputStream;
        private final MIParser miParser = new MIParser();
        /**
         * List of out of band records since the last result record. Out of band
         * records are required for processing the results of CLI commands.
         */
        private final List<MIOOBRecord> fAccumulatedOOBRecords = new LinkedList<MIOOBRecord>();
        /**
         * List of stream records since the last result record, not including
         * the record currently being processed (if it's a stream one). This is
         * a subset of {@link #fAccumulatedOOBRecords}, as a stream record is a
         * particular type of OOB record.
         */
        private final List<MIStreamRecord> fAccumulatedStreamRecords = new LinkedList<MIStreamRecord>();


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
            Utils.debug(this.getClass().getSimpleName() + " -- reading: " + line);

            MIParser.RecordType recordType = miParser.getRecordType(line);
            if (recordType == MIParser.RecordType.ResultRecord) {
                final MIResultRecord rr = miParser.parseMIResultRecord(line);

                /*
                 *  Find the command in the current output list. If we cannot then this is
                 *  some form of asynchronous notification. Or perhaps general IO.
                 */
                int id = rr.getToken();

                final CommandHandle commandHandle = rxCommands.remove(id);

                if (commandHandle != null) {
                    final MIOutput response = new MIOutput(
                            rr, fAccumulatedOOBRecords.toArray(new MIOOBRecord[fAccumulatedOOBRecords.size()]) );
                    fAccumulatedOOBRecords.clear();
                    fAccumulatedStreamRecords.clear();

                    MIInfo result = commandHandle.getCommand().getResult(response);
//                    DataRequestMonitor<MIInfo> rm = commandHandle.getRequestMonitor();

                    /*
                     *  Not all users want to get there results. They indicate so by not having
                     *  a completion object.
                     */
//                    if ( rm != null ) {
                    if (client != null) {
                        notifyClient(result);
//                        rm.setData(result);

//                        /*
//                         * We need to indicate if this request had an error or not.
//                         */
//                        String errorResult =  rr.getResultClass();

//                        if ( errorResult.equals(MIResultRecord.ERROR) ) {
//                            String status = getStatusString(commandHandle.getCommand(),response);
//                            String message = getBackendMessage(response);
//                            Exception exception = new Exception(message);
//                            rm.setStatus(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID, REQUEST_FAILED, status, exception));
//                        }

//                        /*
//                         *  We need to complete the command on the DSF thread for data security.
//                         */
//                        final ICommandResult finalResult = result;
//                        getExecutor().execute(new DsfRunnable() {
//                            @Override
//                            public void run() {
//                                /*
//                                 *  Complete the specific command.
//                                 */
//                                if (commandHandle.getRequestMonitor() != null) {
//                                    commandHandle.getRequestMonitor().done();
//                                }

//                                /*
//                                 *  Now tell the generic listeners about it.
//                                 */
//                                processCommandDone(commandHandle, finalResult);
//                            }
//                            @Override
//                            public String toString() {
//                                return "MI command output received for: " + commandHandle.getCommand(); //$NON-NLS-1$
//                            }
//                        });
                    } else {
                        Utils.debug(this.getClass().getSimpleName() + " -- processCommandDone: " + response.toString());
                        /*
                         *  While the specific requestor did not care about the completion  we
                         *  need to call any listeners. This could have been a CLI command for
                         *  example and  the CommandDone listeners there handle the IO as part
                         *  of the work.
                         */
//                        final ICommandResult finalResult = result;
//                        getExecutor().execute(new DsfRunnable() {
//                            @Override
//                            public void run() {
//                                processCommandDone(commandHandle, finalResult);
//                            }
//                            @Override
//                            public String toString() {
//                                return "MI command output received for: " + commandHandle.getCommand(); //$NON-NLS-1$
//                            }
//                        });
                    }
                } else {
                    /*
                     *  GDB apparently can sometimes send multiple responses to the same command.  In those cases,
                     *  the command handle is gone, so post the result as an event.  To avoid processing OOB records
                     *  as events multiple times, do not include the accumulated OOB record list in the response
                     *  MIOutput object.
                     */
                    final MIOutput response = new MIOutput(rr, new MIOOBRecord[0]);
                    processEvent(response);

//                    getExecutor().execute(new DsfRunnable() {
//                        @Override
//                        public void run() {
//                            processEvent(response);
//                        }
//                        @Override
//                        public String toString() {
//                            return "MI asynchronous output received: " + response; //$NON-NLS-1$
//                        }
//                    });
                }
            } else if (recordType == MIParser.RecordType.OOBRecord) {
                // Process OOBs
                final MIOOBRecord oob = miParser.parseMIOOBRecord(line);

                fAccumulatedOOBRecords.add(oob);
                // limit growth, but only if these are not responses to CLI commands
                // Bug 302927 & 330608
                if (rxCommands.isEmpty() && fAccumulatedOOBRecords.size() > 20) {
                    fAccumulatedOOBRecords.remove(0);
                }

                // The handling of this OOB record may need the stream records
                // that preceded it. One such case is a stopped event caused by a
                // catchpoint in gdb < 7.0. The stopped event provides no
                // reason, but we can determine it was caused by a catchpoint by
                // looking at the target stream.

                final MIOutput response = new MIOutput(oob, fAccumulatedStreamRecords.toArray(new MIStreamRecord[fAccumulatedStreamRecords.size()]));

                // If this is a stream record, add it to the accumulated bucket
                // for possible use in handling a future OOB (see comment above)
                if (oob instanceof MIStreamRecord) {
                    fAccumulatedStreamRecords.add((MIStreamRecord)oob);
                    if (fAccumulatedStreamRecords.size() > 20) { // limit growth; see bug 302927
                        fAccumulatedStreamRecords.remove(0);
                    }
                }

                if (client != null) {
                    processEvent(response);
                }

                /*
                 *   OOBS are events. So we pass them to any event listeners who want to see them. Again this must
                 *   be done on the DSF thread for integrity.
                 */
//                getExecutor().execute(new DsfRunnable() {
//                    @Override
//                    public void run() {
//                        processEvent(response);
//                    }
//                    @Override
//                    public String toString() {
//                        return "MI asynchronous output received: " + response; //$NON-NLS-1$
//                    }
//                });
            }

//            getExecutor().execute(new DsfRunnable() {
//                @Override
//                public void run() {
//                    processNextQueuedCommand();
//                }
//            });
        }

        private void processEvent(MIOutput response) {
            Utils.debug(this.getClass().getSimpleName() + " -- processEvent of: " + response.toString());
        }

        private void notifyClient(MIInfo result) {
            Utils.debug(this.getClass().getSimpleName() + " -- notifyClient of: " + result.toString());
        }
    }

    /**
     * Wrapper for handling individual requests
     */
    private class CommandHandle {
        private MICommand command;
        private int tokenId;

        public CommandHandle(MICommand command) {
            this.command = command;
            tokenId = -1; // only intialized if needed
        }

        public MICommand getCommand() {
            return command;
        }

        public void setCommand(MICommand command) {
            this.command = command;
        }

        public int getTokenId() {
            return tokenId;
        }

        public void setTokenId(int tokenId) {
            this.tokenId = tokenId;
        }

        public void generateTokenId() {
            tokenId = getNewTokenId();}
    }
}
