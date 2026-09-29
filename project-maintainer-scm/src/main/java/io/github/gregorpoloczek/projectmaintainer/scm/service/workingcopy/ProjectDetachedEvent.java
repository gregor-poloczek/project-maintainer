package io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy;

import io.github.gregorpoloczek.projectmaintainer.core.common.events.DomainObjectUpdatedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;

public class ProjectDetachedEvent extends DomainObjectUpdatedEvent<FQPN, FQPN> implements ProjectRelatable {

    public ProjectDetachedEvent(ProjectRelatable projectRelatable) {
        super(projectRelatable.getFQPN(), projectRelatable.getFQPN());
    }

    @Override
    public FQPN getFQPN() {
        return this.getId();
    }
}
