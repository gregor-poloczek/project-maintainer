package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.git.common.Commit;
import java.io.File;
import java.net.URI;
import lombok.Getter;

@Getter

public class ProjectImpl implements Project, GitClonable {

  private volatile boolean cloned;
  private Commit latestCommit;

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
  public void markAsCloned() {
    this.cloned = true;
  }

  @Override
  public FQPN getFQPN() {
    return this.fqpn;
  }

  public void setLatestCommit(Commit commit) {
    this.latestCommit = commit;
  }
}
