package de.gregorpoloczek.projectmaintainer.core.domain.discovery.service;

import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.DiscoveredProject.DiscoveredProjectBuilder;

import java.util.function.Consumer;

public interface ProjectDiscoveryContext {

    void discovered(Consumer<DiscoveredProjectBuilder> builderCallback);

}
