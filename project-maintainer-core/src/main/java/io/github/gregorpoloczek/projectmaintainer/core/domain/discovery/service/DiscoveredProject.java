package io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;

import java.net.URI;
import java.util.Optional;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class DiscoveredProject {

    private final URI uri;
    private final String name;
    private String description;
    private final String owner;
    private String browserLink;
    private String websiteLink;

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<String> getBrowserLink() {
        return Optional.ofNullable(browserLink);
    }

    public Optional<String> getWebsiteLink() {
        return Optional.ofNullable(websiteLink);
    }

    private FQPN fqpn;

    public FQPN getFQPN() {
        return fqpn;
    }

    public URI getURI() {
        return uri;
    }

}
