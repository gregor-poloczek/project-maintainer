package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import java.net.URI;
import java.nio.file.Path;
import lombok.Getter;


@Getter
public class CloneTarget {

  public CloneTarget(final URI uri,
      final Path path) {
    this.uri = uri;
    this.path = path;
  }

  private final URI uri;
  private final Path path;

  public String getFQPN() {
    return path.toString();
  }
}
