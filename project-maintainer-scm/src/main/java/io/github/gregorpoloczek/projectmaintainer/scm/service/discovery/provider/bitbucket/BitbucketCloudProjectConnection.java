package io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket;

import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.facets.GitUsernamePasswordCredentialsFacet;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import java.util.Optional;

@ToString(onlyExplicitlyIncluded = true)
@Value
public class BitbucketCloudProjectConnection implements ProjectConnection {
    public static final String TYPE = "bitbucket-cloud";

    @ToString.Include
    @NonNull
    String id;

    @ToString.Include
    @NonNull
    String email;

    @ToString.Include
    @NonNull
    String bitbucketUsername;

    @NonNull
    String password;

    @ToString.Include
    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public <C> Optional<C> getFacet(Class<C> facetClass) {
        if (facetClass == GitUsernamePasswordCredentialsFacet.class) {
            return Optional.of(new GitUsernamePasswordCredentialsFacet(bitbucketUsername, password))
                    .map(facetClass::cast);
        }

        return Optional.empty();
    }

}
