package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events;


import io.github.gregorpoloczek.projectmaintainer.core.common.events.DomainObjectUpdatedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;

public class ProjectConnectionUpdatedEvent extends DomainObjectUpdatedEvent<String, ProjectConnection> {
    public ProjectConnectionUpdatedEvent(ProjectConnection projectConnection) {
        super(projectConnection.getId(), projectConnection);
    }
}
