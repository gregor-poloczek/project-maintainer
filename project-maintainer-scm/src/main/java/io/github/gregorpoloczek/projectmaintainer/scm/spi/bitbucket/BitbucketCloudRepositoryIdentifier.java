package io.github.gregorpoloczek.projectmaintainer.scm.spi.bitbucket;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;

/**
 * Identifies a repository in Bitbucket Cloud, as required for calls to the Bitbucket Cloud API.
 */
public interface BitbucketCloudRepositoryIdentifier extends ProjectRelatable {

    /**
     * Creates the identifier of the repository the given project refers to.
     *
     * @param projectRelatable a project discovered via a Bitbucket Cloud project connection, or anything relating to it
     * @return the identifier, never {@code null}
     * @throws BitbucketCloudRepositoryIdentifierInvalidException if the {@link FQPN} of the project does not consist
     *                                                            of the expected segments
     */
    static BitbucketCloudRepositoryIdentifier of(ProjectRelatable projectRelatable) {
        return BitbucketCloudRepositoryIdentifierImpl.of(projectRelatable);
    }

    /**
     * Returns the Bitbucket Cloud workspace the repository belongs to (not to be confused with a workspace of this
     * application).
     *
     * @return the Bitbucket Cloud workspace, never {@code null}
     */
    String getBitbucketWorkspace();

    /**
     * Returns the key of the Bitbucket Cloud project the repository belongs to (not to be confused with a project of
     * this application).
     *
     * @return the Bitbucket Cloud project key, never {@code null}
     */
    String getBitbucketProjectKey();

    /**
     * Returns the name of the repository.
     *
     * @return the repository name, never {@code null}
     */
    String getBitbucketSlug();

    /**
     * Returns the {@link FQPN} of the project this identifier was created from.
     *
     * @return the FQPN, never {@code null}
     */
    @Override
    FQPN getFQPN();
}
