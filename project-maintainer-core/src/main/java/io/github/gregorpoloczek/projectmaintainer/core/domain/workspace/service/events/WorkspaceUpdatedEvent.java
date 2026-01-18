package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events;


import io.github.gregorpoloczek.projectmaintainer.core.common.events.DomainObjectUpdatedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.Workspace;

public class WorkspaceUpdatedEvent extends DomainObjectUpdatedEvent<String, Workspace> {
    public WorkspaceUpdatedEvent(Workspace workspace) {
        super(workspace.getId(), workspace);
    }
}
