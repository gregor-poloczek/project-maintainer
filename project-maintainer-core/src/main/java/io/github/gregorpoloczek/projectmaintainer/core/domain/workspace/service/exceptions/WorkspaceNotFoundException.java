package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.exceptions;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WorkspaceNotFoundException extends RuntimeException {
    String id;

    public WorkspaceNotFoundException(String id) {
        super("Workspace with id \"%s\" not found".formatted(id));
        this.id = id;
    }
}
