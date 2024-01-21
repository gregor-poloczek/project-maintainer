package de.gregorpoloczek.projectmaintainer.core.domain.git.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;

public class ProjectAlreadyClonedException extends RuntimeException {

  public ProjectAlreadyClonedException(final FQPN fqpn) {
    super("Project \"%s\" already cloned.".formatted(fqpn));
  }
}
