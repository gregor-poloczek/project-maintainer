package io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service;

import java.util.List;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectDiscoveryService {

    List<ProjectDiscovery<?>> projectDiscoveries;

    public <T extends ProjectConnection> ProjectDiscoveryResult discoverProjects(T connection, FQPN fqpnPrefix) {
        List<ProjectDiscovery<ProjectConnection>> supportingDiscoveries =
                this.projectDiscoveries.stream()
                        .filter(pD -> pD.supports(connection.getType()))
                        .map(c -> ((ProjectDiscovery<ProjectConnection>) c))
                        .toList();
        final ProjectDiscoveryContextImpl<ProjectConnection> projectDiscoveryContext = new ProjectDiscoveryContextImpl<>(connection, fqpnPrefix);
        for (ProjectDiscovery<ProjectConnection> projectDiscovery : supportingDiscoveries) {
            projectDiscovery.discoverProjects(projectDiscoveryContext);
        }
        return new ProjectDiscoveryResult(List.copyOf(projectDiscoveryContext.getDiscoveredProjects()));
    }

}
