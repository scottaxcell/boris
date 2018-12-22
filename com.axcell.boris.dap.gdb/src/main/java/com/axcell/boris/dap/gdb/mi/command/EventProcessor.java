package com.axcell.boris.dap.gdb.mi.command;

import com.axcell.boris.dap.gdb.mi.event.BreakpointHitEvent;
import com.axcell.boris.dap.gdb.mi.event.Event;
import com.axcell.boris.dap.gdb.mi.event.ExitedEvent;
import com.axcell.boris.dap.gdb.mi.event.RunningEvent;
import com.axcell.boris.dap.gdb.mi.output.*;
import com.axcell.boris.dap.gdb.mi.record.OutOfBandRecord;
import com.axcell.boris.dap.gdb.mi.record.ResultRecord;
import com.axcell.boris.utils.Utils;
import org.eclipse.lsp4j.debug.ContinuedEventArguments;
import org.eclipse.lsp4j.debug.OutputEventArguments;
import org.eclipse.lsp4j.debug.TerminatedEventArguments;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;

public class EventProcessor {
    private IDebugProtocolClient client;

    public IDebugProtocolClient getClient() {
        return client;
    }

    public void setClient(IDebugProtocolClient client) {
        this.client = client;
    }

    public void eventReceived(Output output) {
        ResultRecord resultRecord = output.getResultRecord();
        OutOfBandRecord outOfBandRecord = output.getOutOfBandRecord();
        if (resultRecord != null) {
            processResultRecordEvent(resultRecord);
        }
        else if (outOfBandRecord != null) {
            processOutOfBandResponseEvent(outOfBandRecord);
        }
    }

    private void processOutOfBandResponseEvent(OutOfBandRecord outOfBandRecord) {
        Utils.debug("processing out-of-band record event " + outOfBandRecord.toString());
        if (outOfBandRecord instanceof ExecAsyncOutput) {
            processExecAsyncOutput((ExecAsyncOutput) outOfBandRecord);
        }
        else if (outOfBandRecord instanceof StatusAsyncOutput) {
            // TODO
        }
        else if (outOfBandRecord instanceof NotifyAsyncOutput) {
            // TODO
        }
        else if (outOfBandRecord instanceof ConsoleStreamOutput) {
            processConsoleStreamOutput((ConsoleStreamOutput) outOfBandRecord);
        }
        else if (outOfBandRecord instanceof TargetStreamOutput) {
            processTargetStreamOutput((TargetStreamOutput) outOfBandRecord);
        }
        else if (outOfBandRecord instanceof LogStreamOutput) {
            processLogStreamOutput((LogStreamOutput) outOfBandRecord);
        }
    }

    private void processExecAsyncOutput(ExecAsyncOutput execAsyncOutput) {
        String asyncClass = execAsyncOutput.getAsyncClass();
        if (!"stopped".equals(asyncClass))
            return;

        Result[] results = execAsyncOutput.getResults();
        for (Result result : results) {
            String variable = result.getVariable();
            Value value = result.getValue();
            if (variable.equals("reason")) {
                if (value instanceof MIConst) {
                    String reason = ((MIConst) value).getcString();
                    Event event = createEvent(execAsyncOutput, reason);
                    fireEvent(event);
                }
            }
        }
    }

    private void processConsoleStreamOutput(ConsoleStreamOutput consoleStreamOutput) {
        String output = consoleStreamOutput.getcString();
        notifyClientOfStreamOutput(output, "console");
    }

    private void processTargetStreamOutput(TargetStreamOutput targetStreamOutput) {
        String output = targetStreamOutput.getcString();
        notifyClientOfStreamOutput(output, "target");
    }

    private void processLogStreamOutput(LogStreamOutput logStreamOutput) {
        String output = logStreamOutput.getcString();
        notifyClientOfStreamOutput(output, "log");
    }

    private Event createEvent(ExecAsyncOutput execAsyncOutput, String reason) {
        Event event = null;
        if ("exited-normally".equals(reason) || "exited".equals(reason)) {
            event = ExitedEvent.parse(execAsyncOutput.getResults());
        }
        else if ("breakpoint-hit".equals(reason)) {
            event = BreakpointHitEvent.parse(execAsyncOutput.getResults());
        }

        return event;
    }

    private void processResultRecordEvent(ResultRecord resultRecord) {
        Utils.debug("processing result record event " + resultRecord.toString());
        ResultRecord.ResultClass resultClass = resultRecord.getResultClass();
        switch (resultClass) {
            case CONNECTED:
                // TODO
                break;
            case DONE:
                throw new RuntimeException("why wasn't this ResultRecord handled as a response!?");
            case ERROR:
                // TODO
                break;
            case EXIT:
                notifyClientOfGdbExit();
                break;
            case RUNNING:
                notifyClientOfRunning();
                break;
            default:
                throw new RuntimeException("unexpected ResultClass");
        }
    }

    private void fireEvent(Event event) {
        if (event instanceof ExitedEvent) {
            notifyClientOfExitOutOfBandRecord((ExitedEvent) event);
        }
        else if (event instanceof BreakpointHitEvent) {
            notifyClientOfBreakpointHit((BreakpointHitEvent) event);
        }
    }

    private void notifyClientOfGdbExit() {
        Utils.debug("notifying client of gdb exit");
        if (getClient() == null)
            return;
        getClient().terminated(new TerminatedEventArguments());
    }

    private void notifyClientOfExitOutOfBandRecord(ExitedEvent event) {
        Utils.debug("notifying client of exit with code = " + event.getArgs().getExitCode());
        if (getClient() == null)
            return;
        getClient().exited(event.getArgs());
    }

    private void notifyClientOfBreakpointHit(BreakpointHitEvent event) {
        Utils.debug("notifying client of breakpoint hit");
        if (getClient() == null)
            return;
        getClient().stopped(event.getArgs());
    }

    private void notifyClientOfRunningOutOfBandRecord(RunningEvent event) {
        Utils.debug("notifying client of running");
        if (getClient() == null)
            return;
        getClient().continued(event.getArgs());
    }

    private void notifyClientOfRunning() {
        Utils.debug("notifying client of running");
        if (getClient() == null)
            return;
        getClient().continued(new ContinuedEventArguments());
    }

    public void notifyClientOfInitialized() {
        if (getClient() == null)
            return;
        getClient().initialized();
    }

    private void notifyClientOfStreamOutput(String output, String category) {
        if (getClient() == null)
            return;
        OutputEventArguments outputEventArguments = new OutputEventArguments();
        outputEventArguments.setOutput(output);
        outputEventArguments.setCategory(category);
        getClient().output(outputEventArguments);
    }
}
