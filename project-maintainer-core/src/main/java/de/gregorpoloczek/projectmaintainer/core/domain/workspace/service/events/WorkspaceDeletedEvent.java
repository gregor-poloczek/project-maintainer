package de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events;


import de.gregorpoloczek.projectmaintainer.core.common.events.DomainObjectDeletedEvent;
import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.Workspace;

public class WorkspaceDeletedEvent extends DomainObjectDeletedEvent<String> {
    public WorkspaceDeletedEvent(Workspace workspace) {
        super(workspace.getId());
    }
}
