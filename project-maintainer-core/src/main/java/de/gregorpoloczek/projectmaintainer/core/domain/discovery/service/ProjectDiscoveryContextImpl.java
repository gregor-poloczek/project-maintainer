package de.gregorpoloczek.projectmaintainer.core.domain.discovery.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectDiscoveryContextImpl<T extends ProjectConnection> implements ProjectDiscoveryContext<T> {

    private final FQPN fqpnPrefix;

    public ProjectDiscoveryContextImpl(T connection, FQPN fqpnPrefix) {
        this.connection = connection;
        this.fqpnPrefix = fqpnPrefix;
    }

    T connection;

    List<DiscoveredProject> discoveredProjects = new ArrayList<>();

    @Override
    public void discovered(
            final Consumer<DiscoveredProject.DiscoveredProjectBuilder> builderCallback) {
        final DiscoveredProject.DiscoveredProjectBuilder builder = DiscoveredProject.builder();
        builderCallback.accept(builder);

        DiscoveredProject build = builder.build();
        // prepend additional segments in from of the fqpn, to make them more unique across workspaces and
        // connections
        this.discoveredProjects.add(build.toBuilder().fqpn(this.fqpnPrefix.append(build.getFQPN())).build());
    }
}
