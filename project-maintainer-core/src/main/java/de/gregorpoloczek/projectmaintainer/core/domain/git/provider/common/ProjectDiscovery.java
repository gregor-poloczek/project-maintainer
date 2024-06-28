package de.gregorpoloczek.projectmaintainer.core.domain.git.provider.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectDiscoveryContext;

public interface ProjectDiscovery {

    void discoverProjects(ProjectDiscoveryContext context);

}
