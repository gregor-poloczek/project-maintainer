package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.net.URI;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import org.eclipse.jgit.transport.CredentialsProvider;

@Getter
@Builder
public class DiscoveredProject {

    private final URI uri;
    private final String name;
    @Builder.Default
    private Optional<String> description = Optional.empty();
    private final String owner;
    @Builder.Default
    private Optional<String> browserLink = Optional.empty();

    private FQPN fqpn;

    public FQPN getFQPN() {
        return fqpn;
    }

    public URI getURI() {
        return uri;
    }

    private CredentialsProvider credentialsProvider;
}
