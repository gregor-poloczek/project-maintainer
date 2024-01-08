package de.gregorpoloczek.projectmaintainer.core.domain.project.repository.projectsfile;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.net.URI;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class ProjectJSON {

  private URI uri;
  private FQPN fqpn;

  public ProjectJSON(final URI uri, final FQPN fqpn) {
    this.uri = uri;
    this.fqpn = fqpn;
  }

  public URI getURI() {
    return uri;
  }

  public FQPN getFQPN() {
    return fqpn;
  }

  public ProjectJSON() {

  }


}
