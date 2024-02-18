package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.net.URI;
import java.util.Optional;
import lombok.Getter;
import lombok.NonNull;

public class DiscoveredProjectImpl implements DiscoveredProject {

  @Getter
  private final URI uri;
  @Getter
  private final String name;
  @Getter
  private final Optional<String> description;
  private FQPN fqpn;

  public <T> T getCredentials(Class<? extends T> clazz) {
    return clazz.cast(this.credentials);
  }

  private Object credentials;

  public DiscoveredProjectImpl(
      @NonNull final FQPN fqpn,
      @NonNull final URI uri,
      @NonNull final String name,
      final String description,
      @NonNull final Object credentials) {
    this.fqpn = fqpn;
    this.uri = uri;
    this.name = name;
    this.description = Optional.ofNullable(description);
    this.credentials = credentials;
  }

  @Override
  public URI getURI() {
    return this.uri;
  }

  @Override
  public FQPN getFQPN() {
    return this.fqpn;
  }
}
