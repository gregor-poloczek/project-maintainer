package de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos;

import de.gregorpoloczek.projectmaintainer.core.domain.project.api.resources.GitProvider;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.net.URI;

public interface ProjectMetaData {

    static ProjectMetaDataBuilder builder() {
        // TODO lombok austesten
        return new ProjectMetaDataBuilder();
    }

    String getOwner();

    String getName();

    URI getURI();

    FQPN getFQPN();

    default GitProvider getGitProvider() {
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

    ;
}
