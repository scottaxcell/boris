package com.boris.debug.server.mi.event;

import com.boris.debug.server.mi.output.MIConst;
import com.boris.debug.server.mi.output.Result;
import com.boris.debug.server.mi.output.Value;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.StoppedEventArgumentsReason;

public class BreakpointHitEvent extends Event {
    private StoppedEventArguments args;

    public BreakpointHitEvent() {
        args = new StoppedEventArguments();
        args.setReason(StoppedEventArgumentsReason.BREAKPOINT);

    }

    public StoppedEventArguments getArgs() {
        return args;
    }

    public static BreakpointHitEvent parse(Result[] results) {
        BreakpointHitEvent event = new BreakpointHitEvent();

        for (Result result : results) {
            String variable = result.getVariable();
            Value value = result.getValue();
            String valueStr = "";
            if (value != null && value instanceof MIConst) {
                valueStr = ((MIConst)value).getcString();
            }

            if ("thread-id".equals(variable)) {
                event.getArgs().setThreadId(Long.valueOf(valueStr));
            }
            else if ("stopped-threads".equals(variable)) {
                if ("all".equals(valueStr)) {
                    event.getArgs().setAllThreadsStopped(true);
                }
            }
            else if ("bkptno".equals(variable)) {
                String reason = event.getArgs().getReason();
                event.getArgs().setReason(reason + ";bkptno=" + valueStr);
            }
        }
        return event;
    }
}