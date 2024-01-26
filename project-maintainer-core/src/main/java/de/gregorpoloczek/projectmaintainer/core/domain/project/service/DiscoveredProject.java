package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.net.URI;
import java.util.Optional;

public interface DiscoveredProject {

  URI getURI();

  String getName();

  Optional<String> getDescription();

  FQPN getFQPN();
}
