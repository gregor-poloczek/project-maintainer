package de.gregorpoloczek.projectmaintainer.core.domain.git.service;

public class ProjectPullFailedException extends RuntimeException {

  public ProjectPullFailedException(final Throwable cause) {
    super(cause);
  }
}
