package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events;


import io.github.gregorpoloczek.projectmaintainer.core.common.events.DomainObjectDeletedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.Workspace;

public class WorkspaceDeletedEvent extends DomainObjectDeletedEvent<String> {
    public WorkspaceDeletedEvent(Workspace workspace) {
        super(workspace.getId());
    }
}
