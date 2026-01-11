package de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events;


import de.gregorpoloczek.projectmaintainer.core.common.events.DomainObjectUpdatedEvent;
import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;
import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.Workspace;

public class ProjectConnectionUpdatedEvent extends DomainObjectUpdatedEvent<String, ProjectConnection> {
    public ProjectConnectionUpdatedEvent(ProjectConnection projectConnection) {
        super(projectConnection.getId(), projectConnection);
    }
}
