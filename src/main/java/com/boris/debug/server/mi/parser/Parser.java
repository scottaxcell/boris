package com.boris.debug.server.mi.parser;

import com.boris.debug.server.mi.output.*;
import com.boris.debug.server.mi.record.ResultRecord;
import com.boris.debug.server.mi.record.StreamRecord;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    public enum RecordType {
        OutOfBand,
        Result,
        Stream,
        Async
    }

    public RecordType getRecordType(String line) {
        int i = 0;
        if (Character.isDigit(line.charAt(0))) {
            i = 1;
            while (i < line.length() && Character.isDigit(line.charAt(i)))
                i++;
        }
        if (i < line.length()) {
            if (ResultRecord.RESULT_RECORD_PREFIX == line.charAt(i))
                return RecordType.Result;
            else
                return RecordType.OutOfBand;
//            else if (StreamRecord.CONSOLE_OUTPUT_PREFIX == line.charAt(i)
//                    || StreamRecord.TARGET_OUTPUT_PREFIX == line.charAt(i)
//                    || StreamRecord.LOG_OUTPUT_PREFIX == line.charAt(i)) {
//                return RecordType.Stream;
//            }
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

        Result[] results = parseResults(buffer);
        resultRecord.setResults(results);

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
        while (buffer.length() > 0 && buffer.charAt(0) == ',') {
            buffer.deleteCharAt(0);
            Result result = parseResult(buffer);
            if (result != null)
                results.add(result);
        }
        return results.toArray(new Result[results.size()]);
    }

    private Result parseResult(StringBuffer buffer) {
        Result result = new Result();
        int equal;
        if (buffer.length() > 0 && Character.isLetter(buffer.charAt(0)) && (equal = buffer.indexOf("=")) != -1) {
            String variable = buffer.substring(0, equal);
            result.setVariable(variable);
            buffer.delete(0, equal + 1);
            Value value = parseValue(buffer);
            result.setValue(value);
        }
        else {
            Value value = parseValue(buffer);
            if (value != null)
                result.setValue(value);
            else {
                result.setVariable(buffer.toString());
                result.setValue(new MIConst());
                buffer.setLength(0);
            }
        }
        return result;
    }

    private Value parseValue(StringBuffer buffer) {
        Value value = null;
        if (buffer.length() > 0) {
            if (buffer.charAt(0) == '{') {
                buffer.deleteCharAt(0);
                value = parseTuple(buffer);
            }
            else if (buffer.charAt(0) == '[') {
                buffer.deleteCharAt(0);
                value = parseList(buffer);
            }
            else if (buffer.charAt(0) == '"') {
                buffer.deleteCharAt(0);
                MIConst miConst = new MIConst();
                miConst.setcString(parseCString(buffer));
                value = miConst;
            }
        }
        return value;
    }

    private Tuple parseTuple(StringBuffer buffer) {
        Tuple tuple = new Tuple();
        List<Value> values = new ArrayList<>();
        List<Result> results = new ArrayList<>();

        while (buffer.length() > 0 && buffer.charAt(0) != '}') {
            Value value = parseValue(buffer);
            if (value != null)
                values.add(value);
            else {
                Result result = parseResult(buffer);
                if (result != null)
                    results.add(result);
            }
            if (buffer.length() > 0 && buffer.charAt(0) == ',')
                buffer.deleteCharAt(0);
        }

        if (buffer.length() > 0 && buffer.charAt(0) == '}')
            buffer.deleteCharAt(0);

        tuple.setValues(values.toArray(new Value[values.size()]));
        tuple.setResults(results.toArray(new Result[results.size()]));
        return tuple;
    }

    private MIList parseList(StringBuffer buffer) {
        MIList list = new MIList();
        List<Value> values = new ArrayList<>();
        List<Result> results = new ArrayList<>();

        while (buffer.length() > 0 && buffer.charAt(0) != ']') {
            Value value = parseValue(buffer);
            if (value != null)
                values.add(value);
            else {
                Result result = parseResult(buffer);
                if (result != null)
                    results.add(result);
            }
            if (buffer.length() > 0 && buffer.charAt(0) == ',')
                buffer.deleteCharAt(0);
        }

        if (buffer.length() > 0 && buffer.charAt(0) == ']')
            buffer.deleteCharAt(0);

        list.setValues(values.toArray(new Value[values.size()]));
        list.setResults(results.toArray(new Result[results.size()]));
        return list;
    }

    private String parseCString(StringBuffer buffer) {
        boolean escapeSeen = false;
        boolean endQuotesSeen = false;

        StringBuffer stringBuffer = new StringBuffer();

        int i = 0;
        for (; i < buffer.length() && !endQuotesSeen; i++) {
            char c = buffer.charAt(i);
            if (c == '\\') {
                if (escapeSeen) {
                    stringBuffer.append(c);
                    escapeSeen = false;
                } else
                    escapeSeen = true;
            } else if (c == '"') {
                if (escapeSeen) {
                    stringBuffer.append(c);
                    escapeSeen = false;
                } else
                    endQuotesSeen = true;
            } else {
                if (escapeSeen)
                    stringBuffer.append('\\');
                stringBuffer.append(c);
                escapeSeen = false;
            }
        }
        buffer.delete(0, i);
        return stringBuffer.toString();
    }
}
