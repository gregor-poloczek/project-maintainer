package de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events;


import de.gregorpoloczek.projectmaintainer.core.common.events.DomainObjectUpdatedEvent;
import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.Workspace;

public class WorkspaceUpdatedEvent extends DomainObjectUpdatedEvent<String, Workspace> {
    public WorkspaceUpdatedEvent(Workspace workspace) {
        super(workspace.getId(), workspace);
    }
}
