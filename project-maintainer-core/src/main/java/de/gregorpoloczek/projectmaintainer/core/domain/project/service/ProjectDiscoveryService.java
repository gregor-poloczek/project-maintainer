package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.common.ProjectDiscovery;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectDiscoveryService {

    List<ProjectDiscovery> projectDiscoveries;

    public ProjectDiscoveryResult discoverProjects() {
        final ProjectDiscoveryContextImpl projectDiscoveryContext = new ProjectDiscoveryContextImpl();
        for (ProjectDiscovery d : this.projectDiscoveries) {
            d.discoverProjects(projectDiscoveryContext);
        }
        return new ProjectDiscoveryResult(projectDiscoveryContext.getDiscoveredProjects());
    }
}
