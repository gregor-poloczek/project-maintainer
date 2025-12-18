package de.gregorpoloczek.projectmaintainer.core.domain.project.service.events;


import de.gregorpoloczek.projectmaintainer.core.common.events.DomainObjectUpdatedEvent;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;

public class ProjectUpdatedEvent extends DomainObjectUpdatedEvent<FQPN, Project> {
    public ProjectUpdatedEvent(Project project) {
        super(project.getFQPN(), project);
    }
}
