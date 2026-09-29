package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.exceptions;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectConnectionNotFoundException extends RuntimeException {
    String workspaceId;
    String connectionId;

    public ProjectConnectionNotFoundException(String workspaceId, String connectionId) {
        super("Connection with id \"%s\" not found in workspace \"%s\"".formatted(connectionId, workspaceId));
        this.workspaceId = workspaceId;
        this.connectionId = connectionId;
    }
}
