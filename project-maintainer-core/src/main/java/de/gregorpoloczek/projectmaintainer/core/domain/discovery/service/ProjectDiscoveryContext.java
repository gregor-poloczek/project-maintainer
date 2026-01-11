package de.gregorpoloczek.projectmaintainer.core.domain.discovery.service;

import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.DiscoveredProject.DiscoveredProjectBuilder;
import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;

import java.util.function.Consumer;

public interface ProjectDiscoveryContext<T extends ProjectConnection> {

    T getConnection();

    void discovered(Consumer<DiscoveredProjectBuilder> builderCallback);

}
