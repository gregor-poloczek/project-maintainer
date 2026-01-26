package io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters;

import java.util.Optional;

public interface PatchParameterArgument<T> {
    PatchParameter getParameter();

    Optional<T> getValue();

    default T requireValue() {
        return this.getValue().orElseThrow(() -> new IllegalStateException("No value defined for \"%s\".".formatted(this.getParameter().getId())));
    }

}
