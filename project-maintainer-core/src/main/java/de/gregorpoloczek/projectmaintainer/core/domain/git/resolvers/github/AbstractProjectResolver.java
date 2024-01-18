package de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.github;

import de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.common.GitProjectResolver;
import java.net.URI;
import java.util.Optional;

public abstract class AbstractProjectResolver implements GitProjectResolver {

  protected String requireUsername(final URI uri) {
    return Optional.ofNullable(uri.getUserInfo()).orElseThrow(
        () -> new IllegalArgumentException("Uri " + uri + " does not feature a user."));
  }
}
