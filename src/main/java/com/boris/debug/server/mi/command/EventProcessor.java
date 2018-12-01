package com.boris.debug.server.mi.command;

import com.boris.debug.server.mi.event.Event;
import com.boris.debug.server.mi.output.*;
import com.boris.debug.server.mi.record.OutOfBandRecord;
import com.boris.debug.server.mi.record.ResultRecord;
import com.boris.debug.utils.Utils;
import org.eclipse.lsp4j.debug.ExitedEventArguments;
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
        if (client == null)
            return;

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
        // TODO This should all be passed to the EventProcessor to handle
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
        for (int i = 0; i < results.length; i++) {
            String variable = results[i].getVariable();
            Value value = results[i].getValue();
            if (variable.equals("reason")) {
                if (value instanceof MIConst) {
                    String reason = ((MIConst) value).getcString();
                    if ("exited-normally".equals(reason) || "exited".equals(reason)) {
                        Event event = createEvent(execAsyncOutput, reason);
                        // TODO pass event to some sort of event process to notify the client
                    }
                }
            }
        }
    }

    private Event createEvent(ExecAsyncOutput execAsyncOutput, String reason) {

        return null;
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
                notifyClientOfExitResultRecord();
                break;
            case RUNNING:
                // TODO
                break;
            default:
                throw new RuntimeException("unexpected ResultClass");
        }

    }

    private void notifyClientOfExitResultRecord() {
        if (client == null)
            return;
        TerminatedEventArguments terminatedEventArguments = new TerminatedEventArguments();
        client.terminated(terminatedEventArguments);
    }

    private void notifyClientOfExitOutOfBandRecord(Result[] results) {
        if (client == null)
            return;
        ExitedEventArguments exitedEventArguments = new ExitedEventArguments();
        client.exited(exitedEventArguments);
    }

}
