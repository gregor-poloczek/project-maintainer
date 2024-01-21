package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import java.util.function.Consumer;

public interface ProjectDiscoveryContext {

  void discovered(Consumer<DiscoveredProjectBuilder> builderCallback);

}
