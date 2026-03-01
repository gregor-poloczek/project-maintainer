package io.github.gregorpoloczek.projectmaintainer.patching.common;

import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.facets.GitUsernamePasswordCredentialsFacet;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.experimental.FieldDefaults;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Builder
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IntegrationTestFileSystemProjectConnection implements ProjectConnection {

    public static final String TYPE = "integration-tests";

    @NonNull
    @Getter
    @Builder.Default
    String id = UUID.randomUUID().toString();

    @Singular
    @Getter
    private List<Path> remoteRepositories;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public <C> Optional<C> getFacet(Class<C> facetClass) {
        if (facetClass == GitUsernamePasswordCredentialsFacet.class) {
            return Optional.of(new GitUsernamePasswordCredentialsFacet("", ""))
                    .map(facetClass::cast);
        }
        return Optional.empty();
    }
}