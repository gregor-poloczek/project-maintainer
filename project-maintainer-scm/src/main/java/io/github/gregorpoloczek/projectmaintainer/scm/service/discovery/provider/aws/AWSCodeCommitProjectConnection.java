package io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.aws;

import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.facets.GitUsernamePasswordCredentialsFacet;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import java.util.Optional;

@ToString(onlyExplicitlyIncluded = true)
@Value
public class AWSCodeCommitProjectConnection implements ProjectConnection {
    public static final String TYPE = "aws-codecommit";

    @ToString.Include
    @NonNull
    String id;

    @ToString.Include
    @NonNull
    String username;

    @ToString.Exclude
    @NonNull
    String password;

    @ToString.Include
    @NonNull
    String profile;

    @ToString.Include
    @NonNull
    String region;

    @ToString.Include
    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public <C> Optional<C> getFacet(Class<C> facetClass) {
        if (facetClass == GitUsernamePasswordCredentialsFacet.class) {
            return Optional.of(new GitUsernamePasswordCredentialsFacet(username, password))
                    .map(facetClass::cast);
        }

        return Optional.empty();
    }

}
