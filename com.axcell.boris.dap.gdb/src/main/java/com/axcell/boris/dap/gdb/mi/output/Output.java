package com.axcell.boris.dap.gdb.mi.output;

import com.axcell.boris.dap.gdb.mi.record.OutOfBandRecord;
import com.axcell.boris.dap.gdb.mi.record.ResultRecord;

/**
 * output -> ( out-of-band-record )* [ result-record ] "(gdb)" nl
 */
public class Output {
    // TODO treat out of band records as * instead of just 1
    private OutOfBandRecord outOfBandRecord;
    private ResultRecord resultRecord;

    public Output(ResultRecord resultRecord) {
        this.resultRecord = resultRecord;
    }

    public Output(OutOfBandRecord outOfBandRecord) {
        this.outOfBandRecord = outOfBandRecord;
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
