package com.boris.debug.server.mi.output;

import com.boris.debug.server.mi.record.OutOfBandRecord;
import com.boris.debug.server.mi.record.ResultRecord;

/**
 * output -> ( out-of-band-record )* [ result-record ] "(gdb)" nl
 */
public class Output {
    private OutOfBandRecord outOfBandRecord;
    private ResultRecord resultRecord;

    public Output(ResultRecord resultRecord) {
        this.resultRecord = resultRecord;
    }

    public Output(OutOfBandRecord outOfBandRecord) {
        this.resultRecord = resultRecord;
    }

    public OutOfBandRecord getOutOfBandRecord() {
        return outOfBandRecord;
    }

    public void setOutOfBandRecord(OutOfBandRecord outOfBandRecord) {
        this.outOfBandRecord = outOfBandRecord;
    }

    public ResultRecord getResultRecord() {
        return resultRecord;
    }

    public void setResultRecord(ResultRecord resultRecord) {
        this.resultRecord = resultRecord;
    }
}
