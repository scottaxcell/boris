package com.boris.debug.server.mi.output;

import com.boris.debug.server.mi.record.ResultRecord;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.Thread;

import java.util.ArrayList;
import java.util.List;

public class OutputParser {
    public static Breakpoint parseBreakpointsResponse(Output output) {
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
                    return breakpoint;
                }
            }
        }

        return null;
    }

    public static ThreadsResponse parseThreadsResponse(Output output) {
        ThreadsResponse response = new ThreadsResponse();
        List<Thread> threads = new ArrayList<>();

        ResultRecord resultRecord = output.getResultRecord();
        if (resultRecord.getResultClass() == ResultRecord.ResultClass.DONE) {
            Result[] results = resultRecord.getResults();
            for (Result result : results) {
                if ("threads".equals(result.getVariable())) {
                    Value value = result.getValue();
                    if (value instanceof MIList) {
                        MIList list = (MIList) value;
                        Value[] values = list.getValues();
                        for (Value v : values) {
                            if (v instanceof Tuple) {
                                org.eclipse.lsp4j.debug.Thread thread = parseThread((Tuple) v);
                                threads.add(thread);
                            }
                        }
                    }
                }
            }
        }

        if (!threads.isEmpty())
            response.setThreads(threads.toArray(new org.eclipse.lsp4j.debug.Thread[threads.size()]));

        return response;
    }

    public static Thread parseThread(Tuple tuple) {
        org.eclipse.lsp4j.debug.Thread thread = new org.eclipse.lsp4j.debug.Thread();
        Result[] tupleResults = tuple.getResults();
        for (Result tr : tupleResults) {
            String var = tr.getVariable();
            if ("id".equals(var)) {
                Value val = tr.getValue();
                if (val instanceof MIConst)
                    thread.setId(Long.valueOf(((MIConst) val).getcString().trim()));
            }
            else if ("name".equals(var)) {
                Value val = tr.getValue();
                if (val instanceof MIConst)
                    thread.setName(((MIConst) val).getcString().trim());
            }
        }
        return thread;
    }
}
