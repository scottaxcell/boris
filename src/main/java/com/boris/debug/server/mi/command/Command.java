package com.boris.debug.server.mi.command;

import java.util.ArrayList;
import java.util.List;

public class Command {
    private String operation;
    private List<String> parameters;
    private List<String> options;
    private boolean requiresResponse;
    private boolean ignoreResponse;

    public Command(String operation) {
        this.operation = operation;
        parameters = new ArrayList<>();
        options = new ArrayList<>();
    }

    public Command(String operation, List<String> parameters) {
        this.operation = operation;
        this.parameters = parameters;
        options = new ArrayList<>();
    }

    public boolean isRequiresResponse() {
        return requiresResponse;
    }

    public void setRequiresResponse(boolean requiresResponse) {
        this.requiresResponse = requiresResponse;
    }

    public boolean isIgnoreResponse() {
        return ignoreResponse;
    }

    public void setIgnoreResponse(boolean ignoreResponse) {
        this.ignoreResponse = ignoreResponse;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String constructCommand() {
        StringBuilder command = new StringBuilder(getOperation());
        // TODO --thread, --thread-group, and --frame support

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
        for (String option : options)
            stringBuilder.append(' ').append(option);
        return stringBuilder.toString().trim();
    }

    private String parametersToString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (String parameter : parameters)
            stringBuilder.append(' ').append(parameter);
        return stringBuilder.toString().trim();
    }
}
