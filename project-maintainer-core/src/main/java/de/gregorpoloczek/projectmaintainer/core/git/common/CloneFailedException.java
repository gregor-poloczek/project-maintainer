package de.gregorpoloczek.projectmaintainer.core.git.common;

public class CloneFailedException extends RuntimeException {

  public CloneFailedException(final Throwable cause) {
    super(cause);
  }
}
