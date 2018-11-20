package com.boris.debug.server.commands;

import com.boris.debug.server.output.MIInfo;
import com.boris.debug.server.output.MIOutput;

import java.util.ArrayList;
import java.util.List;
public class MICommand {
    private String operation;
    List<String> options = new ArrayList<>();
    List<String> parameters = new ArrayList<>();

    public MICommand(String operation) {
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }

    public List<String> getOptions() {
        return options;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    public String constructCommand() {
        StringBuilder command = new StringBuilder(getOperation());

        // TODO add --thread, --thread-group, and --frame support

        String options = optionsToString();
        if (!options.isEmpty())
            command.append(' ').append(options);

        String parameters = parametersToString();
        if (!parameters.isEmpty())
            command.append(' ').append(parameters);

        command.append('\n');
        return command.toString();
    }

    private String optionsToString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (String option : options) {
            stringBuilder.append(' ').append(option);
        }
        return stringBuilder.toString().trim();
    }

    private String parametersToString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (String parameter : parameters) {
            stringBuilder.append(' ').append(parameter);
        }
        return stringBuilder.toString().trim();
    }

    public MIInfo getResult(MIOutput MIresult) {
        return new MIInfo(MIresult);
    }
}
