package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.net.URI;

public interface DiscoveredProjectBuilder {

  DiscoveredProjectBuilder uri(URI uri);

  DiscoveredProjectBuilder fqpn(FQPN fqpn);

  DiscoveredProjectBuilder name(String name);

  DiscoveredProjectBuilder description(String description);

  default DiscoveredProjectBuilder uri(String uri) {
    return this.uri(URI.create(uri));
  }

  DiscoveredProjectBuilder credentials(Object credentials);
}
