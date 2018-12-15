package com.boris.debug.server.mi.command;

public class CommandFactory {

    public BreakInsertCommand createBreakInsert(String location) {
        BreakInsertCommand breakInsertCommand = new BreakInsertCommand(location);
        breakInsertCommand.setRequiresResponse(true);
        return breakInsertCommand;
    }

    public GdbExitCommand createGdbExit() {
        return new GdbExitCommand();
    }

    public ExecRunCommand createExecRun() {
        return new ExecRunCommand();
    }

    public ExecContinueCommand createExecContinue() {
        ExecContinueCommand execContinueCommand = new ExecContinueCommand();
        execContinueCommand.setRequiresResponse(true);
        return execContinueCommand;
    }

    public ThreadsInfoCommand createThreadsInfo() {
        ThreadsInfoCommand threadsInfoCommand = new ThreadsInfoCommand();
        threadsInfoCommand.setRequiresResponse(true);
        return threadsInfoCommand;
    }

    public StackListFramesCommand createStackListFrames() {
        StackListFramesCommand stackListFramesCommand = new StackListFramesCommand();
        stackListFramesCommand.setRequiresResponse(true);
        return stackListFramesCommand;
    }

    public ThreadSelectCommand createThreadSelect(Long threadNum) {
        ThreadSelectCommand threadSelectCommand = new ThreadSelectCommand(String.valueOf(threadNum));
        threadSelectCommand.setRequiresResponse(true);
        return threadSelectCommand;
    }
}
