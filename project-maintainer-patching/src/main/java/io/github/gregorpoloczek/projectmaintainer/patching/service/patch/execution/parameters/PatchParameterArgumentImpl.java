package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.parameters;


import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameter;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.PatchParameterArgument;

import java.util.Optional;

public class PatchParameterArgumentImpl<T> implements PatchParameterArgument<T> {
    private final T value;
    private final PatchParameter parameter;

    public PatchParameterArgumentImpl(PatchParameter parameter, T value) {
        this.parameter = parameter;
        this.value = value;
    }

    public PatchParameter getParameter() {
        return parameter;
    }

    public Optional<T> getValue() {
        return Optional.ofNullable(value);
    }

    @Override
    public T requireValue() {
        return Optional.ofNullable(value).orElseThrow(() -> new IllegalStateException("No value defined for parameter \"%s\".".formatted(this.parameter.getId())));
    }

}
