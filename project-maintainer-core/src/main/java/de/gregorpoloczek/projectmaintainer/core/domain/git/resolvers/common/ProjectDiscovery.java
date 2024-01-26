package de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectDiscoveryContext;
import java.net.URI;
import java.util.List;

public interface ProjectDiscovery {

  default void discoverProjects(ProjectDiscoveryContext context) {
    this.getURIs().forEach(uri -> context.discovered(b -> b.uri(uri)));
  }

  List<URI> getURIs();
}
