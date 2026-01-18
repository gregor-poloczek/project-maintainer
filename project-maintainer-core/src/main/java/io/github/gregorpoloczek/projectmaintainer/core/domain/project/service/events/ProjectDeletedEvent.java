package io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.events;


import io.github.gregorpoloczek.projectmaintainer.core.common.events.DomainObjectDeletedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;

public class ProjectDeletedEvent extends DomainObjectDeletedEvent<FQPN> {
    public ProjectDeletedEvent(Project project) {
        super(project.getFQPN());
    }
}
