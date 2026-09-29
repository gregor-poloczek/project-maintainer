package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.exceptions;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WorkspaceNameInvalidException extends RuntimeException {
    String name;

    public WorkspaceNameInvalidException(String name) {
        super("Workspace name is invalid: \"%s\"".formatted(name));
        this.name = name;
    }
}
