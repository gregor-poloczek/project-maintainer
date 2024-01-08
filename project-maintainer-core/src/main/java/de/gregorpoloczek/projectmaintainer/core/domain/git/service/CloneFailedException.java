package de.gregorpoloczek.projectmaintainer.core.domain.git.service;

public class CloneFailedException extends RuntimeException {

  public CloneFailedException(final Throwable cause) {
    super(cause);
  }
}
