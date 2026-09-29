package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.exceptions;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WorkspaceAlreadyExistsException extends RuntimeException {
    String name;

    public WorkspaceAlreadyExistsException(String name) {
        super("Workspace already exists: " + name);
        this.name = name;
    }
}
