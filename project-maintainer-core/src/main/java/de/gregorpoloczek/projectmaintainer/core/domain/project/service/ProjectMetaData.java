package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import java.net.URI;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectMetaData {

    private String owner;
    private String name;
    private Optional<String> description = Optional.empty();
    private URI uri;
    @Builder.Default
    private Optional<String> browserLink = Optional.empty();
    private FQPN fqpn;

    public FQPN getFQPN() {
        return fqpn;
    }

    public URI getURI() {
        return uri;
    }

    public GitProvider getGitProvider() {
        final GitProvider provider;
        if (this.getFQPN().getValue().startsWith("github:")) {
            provider = GitProvider.GITHUB;
        } else if (this.getFQPN().getValue().startsWith("aws-codecommit")) {
            provider = GitProvider.AWS_CODECOMMIT;
        } else if (this.getFQPN().getValue().startsWith("bitbucket")) {
            provider = GitProvider.BITBUCKET;
        } else {
            provider = GitProvider.UNKNOWN;
        }
        return provider;
    }

}
