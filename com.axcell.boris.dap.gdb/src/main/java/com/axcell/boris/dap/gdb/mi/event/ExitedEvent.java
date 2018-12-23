package com.axcell.boris.dap.gdb.mi.event;

import com.axcell.boris.dap.gdb.mi.output.MIConst;
import com.axcell.boris.dap.gdb.mi.output.Result;
import com.axcell.boris.dap.gdb.mi.output.Value;
import org.eclipse.lsp4j.debug.ExitedEventArguments;

public class ExitedEvent extends Event {
    private ExitedEventArguments args;

    public ExitedEvent(Long exitCode) {
        args = new ExitedEventArguments();
        args.setExitCode(exitCode);
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
                }
                catch (NumberFormatException ignored) {
                }
            }
        }
        return new ExitedEvent(code);
    }

    public ExitedEventArguments getArgs() {
        return args;
    }
}
