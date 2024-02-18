package de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectDiscoveryContext;

public interface ProjectDiscovery {

  void discoverProjects(ProjectDiscoveryContext context);

}
