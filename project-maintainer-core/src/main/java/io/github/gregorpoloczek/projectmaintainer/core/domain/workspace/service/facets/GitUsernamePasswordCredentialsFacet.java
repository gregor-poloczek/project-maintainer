package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.facets;

import lombok.Value;

@Value
public class GitUsernamePasswordCredentialsFacet {
    String username;
    String password;
}
