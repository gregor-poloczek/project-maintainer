package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events;


import io.github.gregorpoloczek.projectmaintainer.core.common.events.DomainObjectDeletedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;

public class ProjectConnectionDeletedEvent extends DomainObjectDeletedEvent<String> {
    public ProjectConnectionDeletedEvent(ProjectConnection projectConnection) {
        super(projectConnection.getId());
    }
}
