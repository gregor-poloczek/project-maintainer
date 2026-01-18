package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events;


import io.github.gregorpoloczek.projectmaintainer.core.common.events.DomainObjectCreatedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;

public class ProjectConnectionCreatedEvent extends DomainObjectCreatedEvent<String, ProjectConnection> {
    public ProjectConnectionCreatedEvent(ProjectConnection projectConnection) {
        super(projectConnection.getId(), projectConnection);
    }
}
