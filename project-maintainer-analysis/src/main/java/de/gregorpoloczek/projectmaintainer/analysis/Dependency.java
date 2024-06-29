package de.gregorpoloczek.projectmaintainer.analysis;

import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Dependency {

    String name;
    String type;
    @Builder.Default
    Optional<String> version = Optional.empty();
}
