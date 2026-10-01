package io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.exceptions;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * Thrown when the working copy of a project is required, but the project is not attached (i.e. has no working copy).
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WorkingCopyNotFoundException extends RuntimeException implements ProjectRelatable {
    FQPN fqpn;

    public WorkingCopyNotFoundException(FQPN fqpn) {
        super("No working copy found for project \"%s\", the project is not attached".formatted(fqpn));
        this.fqpn = fqpn;
    }

    @Override
    public FQPN getFQPN() {
        return this.fqpn;
    }
}
