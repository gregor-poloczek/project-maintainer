package de.gregorpoloczek.projectmaintainer.core.domain.git.service;

public class PullFailedException extends RuntimeException {

  public PullFailedException(final Throwable cause) {
    super(cause);
  }
}
