package io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service;

import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.DiscoveredProject.DiscoveredProjectBuilder;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;

import java.util.function.Consumer;

public interface ProjectDiscoveryContext<T extends ProjectConnection> {

    T getConnection();

    void discovered(Consumer<DiscoveredProjectBuilder> builderCallback);

}
