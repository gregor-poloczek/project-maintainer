package de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.bitbucket;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RepositoryLinksResource(
        @JsonProperty("clone")
        List<RepositoryLinkResource> klone) {

}
