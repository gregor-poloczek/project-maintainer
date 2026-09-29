package io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.events;


import io.github.gregorpoloczek.projectmaintainer.core.common.events.DomainObjectDeletedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;

public class ProjectDeletedEvent extends DomainObjectDeletedEvent<FQPN> implements ProjectRelatable {
    public ProjectDeletedEvent(Project project) {
        super(project.getFQPN());
    }

    @Override
    public FQPN getFQPN() {
        return this.getId();
    }
}
