package de.gregorpoloczek.projectmaintainer.reporting.config;

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
    @NotEmpty
    String textAlignment = "left";
}
