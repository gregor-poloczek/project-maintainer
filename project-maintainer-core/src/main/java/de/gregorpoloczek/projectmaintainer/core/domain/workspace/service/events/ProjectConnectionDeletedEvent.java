package de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events;


import de.gregorpoloczek.projectmaintainer.core.common.events.DomainObjectDeletedEvent;
import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;

public class ProjectConnectionDeletedEvent extends DomainObjectDeletedEvent<String> {
    public ProjectConnectionDeletedEvent(ProjectConnection projectConnection) {
        super(projectConnection.getId());
    }
}
