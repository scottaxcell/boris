package com.boris.debug.server.mi.output;

import com.boris.debug.server.mi.record.OutOfBandRecord;
import com.boris.debug.server.mi.record.ResultRecord;

/**
 * output -> ( out-of-band-record )* [ result-record ] "(gdb)" nl
 */
public class Output {
    private OutOfBandRecord[] outOfBandRecords;
    private ResultRecord resultRecord;

    public Output(ResultRecord resultRecord) {
        this.resultRecord = resultRecord;
    }

    public OutOfBandRecord[] getOutOfBandRecords() {
        return outOfBandRecords;
    }

    public void setOutOfBandRecords(OutOfBandRecord[] outOfBandRecords) {
        this.outOfBandRecords = outOfBandRecords;
    }

    public ResultRecord getResultRecord() {
        return resultRecord;
    }

    public void setResultRecord(ResultRecord resultRecord) {
        this.resultRecord = resultRecord;
    }
}
