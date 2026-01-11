package de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.github;

import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;
import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.facets.GitUsernamePasswordCredentialsFacet;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import java.util.Optional;

@ToString(onlyExplicitlyIncluded = true)
@Value
public class GithubProjectConnection implements ProjectConnection {
    public static final String TYPE = "github";

    @ToString.Include
    @NonNull
    String id;

    @ToString.Include
    @NonNull
    String username;
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
            return Optional.of(new GitUsernamePasswordCredentialsFacet(username, password))
                    .map(facetClass::cast);
        }

        return Optional.empty();
    }
}
