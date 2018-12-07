package com.boris.debug.server.mi.event;

import com.boris.debug.server.mi.output.MIConst;
import com.boris.debug.server.mi.output.Result;
import com.boris.debug.server.mi.output.Value;
import org.eclipse.lsp4j.debug.ExitedEventArguments;

public class ExitedEvent extends Event {
    private ExitedEventArguments args;

    public ExitedEvent(Long exitCode) {
        args = new ExitedEventArguments();
        args.setExitCode(exitCode);
    }

    public ExitedEventArguments getArgs() {
        return args;
    }

    public static ExitedEvent parse(Result[] results) {
        long code = 0;
        if (results == null)
            return new ExitedEvent(code);

        for (Result result : results) {
            String variable = result.getVariable();
            Value value = result.getValue();
            if ("exit-code".equals(variable) && value instanceof MIConst) {
                String valueStr = ((MIConst) value).getcString();
                try {
                    code = Integer.decode(valueStr.trim()).longValue();
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return new ExitedEvent(code);
    }
}
