package io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.exceptions;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectNotFoundException extends RuntimeException implements ProjectRelatable {
    FQPN fqpn;

    public ProjectNotFoundException(FQPN fqpn) {
        super("Project \"%s\" not found".formatted(fqpn));
        this.fqpn = fqpn;
    }

    @Override
    public FQPN getFQPN() {
        return this.fqpn;
    }
}
