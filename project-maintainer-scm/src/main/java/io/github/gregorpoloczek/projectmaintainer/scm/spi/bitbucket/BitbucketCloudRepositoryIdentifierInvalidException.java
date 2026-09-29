package io.github.gregorpoloczek.projectmaintainer.scm.spi.bitbucket;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * Thrown when a {@link BitbucketCloudRepositoryIdentifier} is requested for a project whose {@link FQPN} does not
 * identify a Bitbucket Cloud repository, e.g. because the project was discovered via another kind of connection.
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BitbucketCloudRepositoryIdentifierInvalidException extends RuntimeException implements ProjectRelatable {
    FQPN fqpn;

    public BitbucketCloudRepositoryIdentifierInvalidException(FQPN fqpn, String reason) {
        super("FQPN \"%s\" does not identify a Bitbucket Cloud repository: %s".formatted(fqpn, reason));
        this.fqpn = fqpn;
    }

    @Override
    public FQPN getFQPN() {
        return this.fqpn;
    }
}
