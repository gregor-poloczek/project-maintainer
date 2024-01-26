package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.Getter;

public class ProjectDiscoveryContextImpl implements ProjectDiscoveryContext {

  @Getter
  private List<DiscoveredProject> discoveredProjects = new ArrayList<>();

  @Override
  public void discovered(
      final Consumer<DiscoveredProjectBuilder> builderCallback) {
    final DiscoveredProjectBuilderImpl builder = new DiscoveredProjectBuilderImpl();
    builderCallback.accept(builder);
    this.discoveredProjects.add(builder.build());
  }
}
