package io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.events;


import io.github.gregorpoloczek.projectmaintainer.core.common.events.DomainObjectUpdatedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;

public class ProjectUpdatedEvent extends DomainObjectUpdatedEvent<FQPN, Project> {
    public ProjectUpdatedEvent(Project project) {
        super(project.getFQPN(), project);
    }
}
