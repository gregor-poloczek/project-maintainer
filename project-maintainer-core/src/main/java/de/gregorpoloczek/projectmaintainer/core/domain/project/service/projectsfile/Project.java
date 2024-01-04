package de.gregorpoloczek.projectmaintainer.core.domain.project.service.projectsfile;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.net.URI;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class Project {

  private URI uri;
  private FQPN fqpn;

  public Project(final URI uri, final FQPN fqpn) {
    this.uri = uri;
    this.fqpn = fqpn;
  }

  public Project() {

  }


}
