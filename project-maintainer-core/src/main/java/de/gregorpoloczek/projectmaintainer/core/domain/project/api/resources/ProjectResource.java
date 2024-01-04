package de.gregorpoloczek.projectmaintainer.core.domain.project.api.resources;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;

public record ProjectResource(FQPN fqpn) {

  public static ProjectResource of(Project project) {
    return new ProjectResource(project.getFQPN());
  }
}
