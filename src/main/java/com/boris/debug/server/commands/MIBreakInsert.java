package com.boris.debug.server.commands;

import java.util.ArrayList;
import java.util.List;

public class MIBreakInsert extends MICommand {
    private String location;

    public MIBreakInsert(String location) {
        super("-break-insert");
        this.location = location;
        setParameters();
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    private void setParameters() {
        List<String> parameters = new ArrayList<>();
        parameters.add(getLocation());
        setParameters(parameters);
    }
}
