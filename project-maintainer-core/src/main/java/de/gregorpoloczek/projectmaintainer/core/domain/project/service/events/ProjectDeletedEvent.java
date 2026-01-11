package de.gregorpoloczek.projectmaintainer.core.domain.project.service.events;


import de.gregorpoloczek.projectmaintainer.core.common.events.DomainObjectDeletedEvent;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;

public class ProjectDeletedEvent extends DomainObjectDeletedEvent<FQPN> {
    public ProjectDeletedEvent(Project project) {
        super(project.getFQPN());
    }
}
