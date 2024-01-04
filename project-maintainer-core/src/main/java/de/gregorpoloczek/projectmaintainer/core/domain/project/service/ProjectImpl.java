package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.io.File;
import java.net.URI;
import lombok.Getter;
import lombok.Setter;

@Getter

public class ProjectImpl implements Project {

  @Setter
  private volatile boolean cloned;

  public ProjectImpl(final File directory, final URI uri, final FQPN fqpn) {
    this.directory = directory;
    this.uri = uri;
    this.fqpn = fqpn;
  }

  private File directory;
  private URI uri;
  private FQPN fqpn;

  public URI getURI() {
    return uri;
  }

  @Override
  public FQPN getFQPN() {
    return this.fqpn;
  }

}
