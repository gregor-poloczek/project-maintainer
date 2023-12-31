package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import java.net.URI;
import java.nio.file.Path;
import lombok.Getter;


@Getter
public class CloneTarget {

  public CloneTarget(final GitSource gitSource, final URI uri,
      final Path path) {
    this.gitSource = gitSource;
    this.uri = uri;
    this.path = path;
  }

  private final GitSource gitSource;
  private final URI uri;
  private final Path path;

  public String getFQPN() {
    return path.toString();
  }
}
