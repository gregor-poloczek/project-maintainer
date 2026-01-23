package io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters;

import java.util.Optional;

public interface PatchParameterArgument<T> {
    PatchParameter getParameter();

    Optional<T> getValue();

    T requireValue();
}
