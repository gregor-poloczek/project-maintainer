package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.common.ProjectDiscovery;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProjectDiscoveryService {

  private final List<ProjectDiscovery> projectDiscoveries;

  public ProjectDiscoveryService(final List<ProjectDiscovery> projectDiscoveries) {
    this.projectDiscoveries = projectDiscoveries;
  }

  public ProjectDiscoveryResult discoverProjects() {
    final ProjectDiscoveryContextImpl projectDiscoveryContext = new ProjectDiscoveryContextImpl();
    for (ProjectDiscovery d : this.projectDiscoveries) {
      d.discoverProjects(projectDiscoveryContext);
    }
    final List<DiscoveredProject> discoveredProjects = projectDiscoveryContext.getDiscoveredProjects();
    return new ProjectDiscoveryResult() {
      @Override
      public List<DiscoveredProject> getDiscoveredProjects() {
        return discoveredProjects;
      }
    };
  }
}
