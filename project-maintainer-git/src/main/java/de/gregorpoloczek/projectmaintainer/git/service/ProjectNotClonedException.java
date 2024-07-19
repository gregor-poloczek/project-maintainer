package de.gregorpoloczek.projectmaintainer.git.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import lombok.NonNull;

public class ProjectNotClonedException extends RuntimeException {

    public ProjectNotClonedException(final @NonNull ProjectRelatable projectRelatable) {
        super("Project %s has not been cloned yet, no working copy present.".formatted(projectRelatable.getFQPN()));
    }
}
