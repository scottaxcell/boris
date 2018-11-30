package com.boris.debug.server.mi.parser;

import com.boris.debug.server.mi.output.Result;
import com.boris.debug.server.mi.record.ResultRecord;
import com.boris.debug.server.mi.record.StreamRecord;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    public enum RecordType {
        Result,
        Stream,
        Async
    }

    public RecordType getRecordType(String line) {
        int i = 0;
        if (Character.isDigit(line.charAt(0))) {
            i = 1;
            while (i < line.length() && Character.isDigit(line.charAt(i))) {
                i++;
            }
        }
        if (i < line.length()) {
            if (ResultRecord.RESULT_RECORD_PREFIX == line.charAt(i)) {
                return RecordType.Result;
            }
            else if (StreamRecord.CONSOLE_OUTPUT_PREFIX == line.charAt(i)
                    || StreamRecord.TARGET_OUTPUT_PREFIX == line.charAt(i)
                    || StreamRecord.LOG_OUTPUT_PREFIX == line.charAt(i)) {
                return RecordType.Stream;
            }
            // TODO handle async record
        }
        throw new RuntimeException("Can't process end of line");
    }

    public ResultRecord processResultRecord(String line) {
        StringBuffer stringBuffer = new StringBuffer(line);
        // TODO parse token
        int token = parseToken(stringBuffer);

        // TODO parse type (done, running, etc.)
        ResultRecord resultRecord = new ResultRecord();
        // TODO parse result values
        return null;
    }

    private int parseToken(StringBuffer buffer) {
        int token = -1;
        if (Character.isDigit(buffer.charAt(0))) {
            int i = 1;
            while (i < buffer.length() && Character.isDigit(buffer.charAt(i))) {
                i++;
            }
            String digits = buffer.substring(0, i);
            try {
                token = Integer.parseInt(digits);
            } catch (NumberFormatException ignored) {
            }
            buffer.delete(0, i);
        }
        return token;
    }

    public ResultRecord parseResultRecord(String line) {
        StringBuffer buffer = new StringBuffer(line);
        int token = parseToken(buffer);

        buffer.deleteCharAt(0); // delete '^'

        ResultRecord resultRecord = new ResultRecord();
        resultRecord.setToken(token);

        parseResultClass(buffer, resultRecord);

        if (buffer.length() > 0 && buffer.charAt(0) == ',') {
            buffer.deleteCharAt(0);
            Result[] res = parseResults(buffer);
            // TODO resultRecord.setResults(res);
        }

        return resultRecord;
    }

    private void parseResultClass(StringBuffer buffer, ResultRecord resultRecord) {
        if (buffer.toString().startsWith(ResultRecord.ResultClass.CONNECTED.getValue())) {
            resultRecord.setResultClass(ResultRecord.ResultClass.CONNECTED);
            buffer.delete(0, ResultRecord.ResultClass.CONNECTED.getValue().length());
        }
        else if (buffer.toString().startsWith(ResultRecord.ResultClass.DONE.getValue())) {
            resultRecord.setResultClass(ResultRecord.ResultClass.DONE);
            buffer.delete(0, ResultRecord.ResultClass.DONE.getValue().length());
        }
        else if (buffer.toString().startsWith(ResultRecord.ResultClass.ERROR.getValue())) {
            resultRecord.setResultClass(ResultRecord.ResultClass.ERROR);
            buffer.delete(0, ResultRecord.ResultClass.ERROR.getValue().length());
        }
        else if (buffer.toString().startsWith(ResultRecord.ResultClass.EXIT.getValue())) {
            resultRecord.setResultClass(ResultRecord.ResultClass.EXIT);
            buffer.delete(0, ResultRecord.ResultClass.EXIT.getValue().length());
        }
        else if (buffer.toString().startsWith(ResultRecord.ResultClass.RUNNING.getValue())) {
            resultRecord.setResultClass(ResultRecord.ResultClass.RUNNING);
            buffer.delete(0, ResultRecord.ResultClass.RUNNING.getValue().length());
        }
        else {
            throw new RuntimeException("Unexpected ResultRecord ResultClass");
        }
    }

    private Result[] parseResults(StringBuffer buffer) {
        List<Result> results = new ArrayList<>();
        // TODO

        return results.toArray(new Result[results.size()]);
    }
}
