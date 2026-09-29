package io.github.gregorpoloczek.projectmaintainer.scm.spi.bitbucket;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BitbucketCloudRepositoryIdentifierTest {

    @Test
    void of_extractsWorkspaceAndRepositoryName() {
        FQPN fqpn = FQPN.of("pm-workspace", "connection", "bb-workspace", "PROJECT", "repository");

        BitbucketCloudRepositoryIdentifier identifier = BitbucketCloudRepositoryIdentifier.of(fqpn);

        assertThat(identifier.getBitbucketWorkspace()).isEqualTo("bb-workspace");
        assertThat(identifier.getBitbucketProjectKey()).isEqualTo("PROJECT");
        assertThat(identifier.getBitbucketSlug()).isEqualTo("repository");
        assertThat(identifier.getFQPN()).isEqualTo(fqpn);
    }

    @Test
    void of_rejectsFqpnWithUnexpectedSegmentCount() {
        FQPN fqpn = FQPN.of("pm-workspace", "connection", "repository");

        assertThatThrownBy(() -> BitbucketCloudRepositoryIdentifier.of(fqpn))
                .isInstanceOfSatisfying(BitbucketCloudRepositoryIdentifierInvalidException.class,
                        e -> assertThat(e.getFQPN()).isEqualTo(fqpn))
                .hasMessageContaining(fqpn.getValue());
    }
}
