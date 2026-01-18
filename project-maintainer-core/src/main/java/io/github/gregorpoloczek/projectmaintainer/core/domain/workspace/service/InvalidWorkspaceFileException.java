package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.nio.file.Path;


@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvalidWorkspaceFileException extends Exception {
    Path workspaceFilePath;

    public InvalidWorkspaceFileException(Path workspaceFilePath, String message) {
        super(message);
        this.workspaceFilePath = workspaceFilePath;
    }
}
