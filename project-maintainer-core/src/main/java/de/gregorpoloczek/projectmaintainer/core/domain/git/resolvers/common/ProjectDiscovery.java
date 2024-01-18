package de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.common;

import java.net.URI;
import java.util.List;

public interface ProjectDiscovery {

  List<URI> getURIs();
}
