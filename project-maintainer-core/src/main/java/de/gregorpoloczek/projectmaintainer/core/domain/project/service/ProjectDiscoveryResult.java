package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectDiscoveryResult {

    List<DiscoveredProject> discoveredProjects;
}
