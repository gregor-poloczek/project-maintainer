package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events;


import io.github.gregorpoloczek.projectmaintainer.core.common.events.DomainObjectCreatedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.Workspace;

public class WorkspaceCreatedEvent extends DomainObjectCreatedEvent<String, Workspace> {
    public WorkspaceCreatedEvent(Workspace workspace) {
        super(workspace.getId(), workspace);
    }
}
