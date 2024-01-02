package de.gregorpoloczek.projectmaintainer.core.git.common;

public class PullFailedException extends RuntimeException {

  public PullFailedException(final Throwable cause) {
    super(cause);
  }
}
