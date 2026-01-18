package io.github.gregorpoloczek.projectmaintainer.reporting.config;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FilePresenceConfig {

    @NotNull
    private String file;
    @NotNull
    private String withText;

}
