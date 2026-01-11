package de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.facets;

import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;

import java.util.function.Supplier;

public interface BelongsToProjectConnection {
    <T extends ProjectConnection> T getProjectConnection();

    static BelongsToProjectConnection of(Supplier<ProjectConnection> projectConnectionSupplier) {
        return new BelongsToProjectConnection() {
            @Override
            public <T extends ProjectConnection> T getProjectConnection() {
                return (T) projectConnectionSupplier.get();
            }
        };
    }
}
