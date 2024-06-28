package de.gregorpoloczek.projectmaintainer.git.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import lombok.NonNull;

public class ProjectNotClonedException extends RuntimeException {

    public ProjectNotClonedException(final @NonNull FQPN fqpn) {
        super("Project %s has not been cloned yet, no working copy present.".formatted(fqpn));
    }
}
