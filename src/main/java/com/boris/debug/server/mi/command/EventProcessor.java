package com.boris.debug.server.mi.command;

import com.boris.debug.server.mi.event.Event;
import com.boris.debug.server.mi.event.ExitedEvent;
import com.boris.debug.server.mi.output.*;
import com.boris.debug.server.mi.record.OutOfBandRecord;
import com.boris.debug.server.mi.record.ResultRecord;
import com.boris.debug.utils.Utils;
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
//        if (client == null)
//            return;

        ResultRecord resultRecord = output.getResultRecord();
        OutOfBandRecord outOfBandRecord = output.getOutOfBandRecord();
        if (resultRecord != null) {
            processResultRecordEvent(resultRecord);
        }
        else if (outOfBandRecord != null) {
            processOutOfbandResponseEvent(outOfBandRecord);
        }

    }

    public void notifyClient() {
        if (client == null)
            return;
    }

    private void processOutOfbandResponseEvent(OutOfBandRecord outOfBandRecord) {
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
                    dispatchEvent(event);
                }
            }
        }
    }

    private Event createEvent(ExecAsyncOutput execAsyncOutput, String reason) {
        Event event = null;
        if ("exited-normally".equals(reason) || "exited".equals(reason)) {
            event = ExitedEvent.parse(execAsyncOutput.getResults());
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
                // TODO
                break;
            default:
                throw new RuntimeException("unexpected ResultClass");
        }
    }

    private void dispatchEvent(Event event) {
        if (event instanceof ExitedEvent) {
            notifyClientOfExitOutOfBandRecord((ExitedEvent) event);
        }
    }

    private void notifyClientOfGdbExit() {
        Utils.debug("notifying client of gdb exit");
        if (client == null)
            return;
        client.terminated(new TerminatedEventArguments());
    }

    private void notifyClientOfExitOutOfBandRecord(ExitedEvent event) {
        Utils.debug("notifying client of exit with code = " + event.getArgs().getExitCode());
        if (client == null)
            return;
        client.exited(event.getArgs());
    }
}