package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.facets;

import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.Workspace;

import java.util.function.Supplier;

public interface BelongsToWorkspace {
    Workspace getWorkspace();

    static BelongsToWorkspace of(Supplier<Workspace> workspaceSupplier) {
        return workspaceSupplier::get;
    }
}
