package io.github.gregorpoloczek.projectmaintainer.reporting.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ColumnConfig {

    @NotEmpty
    String name;
    String labelPresence;
    String labelBase;
    @Valid
    FilePresenceConfig filePresence;
    @NotEmpty
    String textAlignment = "left";
}
