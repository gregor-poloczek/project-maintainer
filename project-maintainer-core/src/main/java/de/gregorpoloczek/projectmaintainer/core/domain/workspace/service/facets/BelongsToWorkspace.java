package de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.facets;

import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.Workspace;

import java.util.function.Supplier;

public interface BelongsToWorkspace {
    Workspace getWorkspace();

    static BelongsToWorkspace of(Supplier<Workspace> workspaceSupplier) {
        return workspaceSupplier::get;
    }
}
