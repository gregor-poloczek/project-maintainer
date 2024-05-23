package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import jakarta.validation.constraints.NotNull;
import java.net.URI;

public class DiscoveredProjectBuilderImpl implements
        DiscoveredProjectBuilder {

    @NotNull
    private URI uri;
    @NotNull
    private String name;
    private String description;
    @NotNull
    private FQPN fqpn;
    @NotNull
    private Object credentials;
    private String owner;

    @Override
    public DiscoveredProjectBuilderImpl uri(final URI uri) {
        this.uri = uri;
        return this;
    }

    @Override
    public DiscoveredProjectBuilderImpl fqpn(final FQPN fqpn) {
        this.fqpn = fqpn;
        return this;
    }

    @Override
    public DiscoveredProjectBuilder name(final String name) {
        this.name = name;
        return this;
    }

    @Override
    public DiscoveredProjectBuilder description(final String description) {
        this.description = description;
        return this;
    }

    @Override
    public DiscoveredProjectBuilder credentials(final Object credentials) {
        this.credentials = credentials;
        return this;
    }

    @Override
    public DiscoveredProjectBuilder owner(String owner) {
        this.owner = owner;
        return this;
    }

    public DiscoveredProject build() {
        // TODO validation

        return new DiscoveredProjectImpl(
                this.fqpn,
                this.owner,
                this.uri, this.name, this.description, this.credentials);
    }
}
