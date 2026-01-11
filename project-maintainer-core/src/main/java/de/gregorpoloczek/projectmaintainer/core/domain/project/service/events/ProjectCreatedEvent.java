package de.gregorpoloczek.projectmaintainer.core.domain.project.service.events;


import de.gregorpoloczek.projectmaintainer.core.common.events.DomainObjectCreatedEvent;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;

public class ProjectCreatedEvent extends DomainObjectCreatedEvent<FQPN, Project> implements ProjectRelatable {
    public ProjectCreatedEvent(Project project) {
        super(project.getFQPN(), project);
    }

    @Override
    public FQPN getFQPN() {
        return this.getId();
    }
}
