package com.axcell.boris.client.debug.dsp;

import com.axcell.boris.client.debug.model.Value;
import com.axcell.boris.client.debug.model.Variable;
import org.eclipse.lsp4j.debug.VariablesArguments;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DSPValue extends DSPDebugElement implements Value {

    private Long variablesReference;
    private String name;
    private String value;

    public DSPValue(DSPDebugElement parent, Long variablesReference, String name, String value) {
        super(parent.getDebugTarget());
        this.variablesReference = variablesReference;
        this.name = name;
        this.value = value;
    }

    @Override
    public String getReferenceTypeName() {
        // TODO
        return null;
    }

    @Override
    public String getValueString() {
        return value;
    }

    @Override
    public Variable[] getVariables() {
        if (!hasVariables())
            return new Variable[0];

        VariablesArguments args = new VariablesArguments();
        args.setVariablesReference(variablesReference);

        CompletableFuture<DSPVariable[]> future = getDebugTarget().getDebugServer().variables(args)
                .thenApply(response -> {
                    List<DSPVariable> variables = new ArrayList<>();
                    for (org.eclipse.lsp4j.debug.Variable variable : response.getVariables())
                        variables.add(new DSPVariable(this, variable.getVariablesReference(), variable.getName(), variable.getValue()));
                    return variables.toArray(new DSPVariable[variables.size()]);
                });
        try {
            return future.get();
        }
        catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new Variable[0];
        }
    }

    @Override
    public boolean hasVariables() {
        return variablesReference != null;
    }
}
