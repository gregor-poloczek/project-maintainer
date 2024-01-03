package de.gregorpoloczek.projectmaintainer.core.domain.project.service.config;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class Project {

  public Project(final String uri, final FQPN fqpn) {
    this.uri = uri;
    this.fqpn = fqpn;
  }

  public Project() {

  }

  private String uri;
  private FQPN fqpn;

}
