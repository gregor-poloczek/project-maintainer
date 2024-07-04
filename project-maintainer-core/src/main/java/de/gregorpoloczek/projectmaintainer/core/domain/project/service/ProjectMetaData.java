package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import java.net.URI;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectMetaData implements HasProjectIdentifier {

    private String owner;
    private String name;
    private String description;
    private URI uri;
    private String browserLink;
    private String websiteLink;
    private FQPN fqpn;

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<String> getBrowserLink() {
        return Optional.ofNullable(browserLink);
    }

    public Optional<String> getWebsiteLink() {
        return Optional.ofNullable(websiteLink);
    }

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
