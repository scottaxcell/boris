package com.boris.debug.server.commands;

public class MICommandFactory {

    public MIBreakInsert createBreakInsert(String location) {
        return new MIBreakInsert(location);
    }

    public MIGdbExit createGdbExit() {
        return new MIGdbExit();
    }

    public MIExecContinue createExecContinue() {
        return new MIExecContinue();
    }

    public MIExecRun createExecRun() {
        return new MIExecRun();
    }

    public MIGdbNext createGdbNext() {
        return new MIGdbNext();
    }

}
