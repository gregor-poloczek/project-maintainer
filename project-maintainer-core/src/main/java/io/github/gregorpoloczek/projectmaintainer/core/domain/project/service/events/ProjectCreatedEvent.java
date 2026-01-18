package io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.events;


import io.github.gregorpoloczek.projectmaintainer.core.common.events.DomainObjectCreatedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;

public class ProjectCreatedEvent extends DomainObjectCreatedEvent<FQPN, Project> implements ProjectRelatable {
    public ProjectCreatedEvent(Project project) {
        super(project.getFQPN(), project);
    }

    @Override
    public FQPN getFQPN() {
        return this.getId();
    }
}
