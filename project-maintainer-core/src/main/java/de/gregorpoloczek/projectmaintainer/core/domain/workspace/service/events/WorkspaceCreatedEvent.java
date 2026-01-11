package de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events;


import de.gregorpoloczek.projectmaintainer.core.common.events.DomainObjectCreatedEvent;
import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.Workspace;

public class WorkspaceCreatedEvent extends DomainObjectCreatedEvent<String, Workspace> {
    public WorkspaceCreatedEvent(Workspace workspace) {
        super(workspace.getId(), workspace);
    }
}
