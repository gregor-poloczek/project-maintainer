package io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Optional;

@Builder
@Value
public class PatchParameter {
    @NonNull
    String id;
    @NonNull
    PatchParameterType type;
    @Builder.Default
    boolean required = true;

    String name;
    String description;

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }
}
