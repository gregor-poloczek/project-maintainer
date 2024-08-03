package de.gregorpoloczek.projectmaintainer.core.domain.discovery.service;

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
        for (ProjectDiscovery projectDiscovery : this.projectDiscoveries) {
            projectDiscovery.discoverProjects(projectDiscoveryContext);
        }
        return new ProjectDiscoveryResult(List.copyOf(projectDiscoveryContext.getDiscoveredProjects()));
    }
}
