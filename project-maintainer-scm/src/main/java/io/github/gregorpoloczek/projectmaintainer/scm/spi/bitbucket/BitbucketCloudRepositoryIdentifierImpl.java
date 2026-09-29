package io.github.gregorpoloczek.projectmaintainer.scm.spi.bitbucket;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Default implementation of {@link BitbucketCloudRepositoryIdentifier}, to be created via
 * {@link BitbucketCloudRepositoryIdentifier#of(ProjectRelatable)}.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class BitbucketCloudRepositoryIdentifierImpl implements BitbucketCloudRepositoryIdentifier {

    private static final int SEGMENT_COUNT = 5;
    private static final int WORKSPACE_INDEX = 2;
    private static final int PROJECT_KEY_INDEX = 3;
    private static final int SLUG_INDEX = 4;

    @Getter(AccessLevel.NONE)
    FQPN fqpn;
    String bitbucketWorkspace;
    String bitbucketProjectKey;
    String bitbucketSlug;

    static BitbucketCloudRepositoryIdentifierImpl of(ProjectRelatable projectRelatable) {
        FQPN fqpn = projectRelatable.getFQPN();
        List<String> segments = fqpn.getSegments();
        if (segments.size() != SEGMENT_COUNT) {
            throw new BitbucketCloudRepositoryIdentifierInvalidException(fqpn,
                    "expected %d segments but got %d".formatted(SEGMENT_COUNT, segments.size()));
        }
        return new BitbucketCloudRepositoryIdentifierImpl(fqpn,
                segments.get(WORKSPACE_INDEX),
                segments.get(PROJECT_KEY_INDEX),
                segments.get(SLUG_INDEX));
    }

    @Override
    public FQPN getFQPN() {
        return this.fqpn;
    }
}
