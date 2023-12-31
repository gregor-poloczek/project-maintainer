package de.gregorpoloczek.projectmaintainer.core.git.common;

import org.eclipse.jgit.api.errors.GitAPIException;

public class CloneFailedException extends RuntimeException {

  public CloneFailedException(final GitAPIException e) {
    super(e);
  }
}
