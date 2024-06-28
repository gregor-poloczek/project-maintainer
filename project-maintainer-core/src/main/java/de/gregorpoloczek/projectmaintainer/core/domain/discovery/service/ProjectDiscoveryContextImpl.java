package de.gregorpoloczek.projectmaintainer.core.domain.discovery.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectDiscoveryContextImpl implements ProjectDiscoveryContext {

    List<DiscoveredProject> discoveredProjects = new ArrayList<>();

    @Override
    public void discovered(
            final Consumer<DiscoveredProject.DiscoveredProjectBuilder> builderCallback) {
        final DiscoveredProject.DiscoveredProjectBuilder builder = DiscoveredProject.builder();
        builderCallback.accept(builder);
        this.discoveredProjects.add(builder.build());
    }
}
