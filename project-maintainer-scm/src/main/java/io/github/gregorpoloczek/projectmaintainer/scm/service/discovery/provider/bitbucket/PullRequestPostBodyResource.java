package io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.api.PullRequestResource;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Builder
class PullRequestPostBodyResource {

    String title;
    PullRequestResource.PullRequestLocation source;
    PullRequestResource.PullRequestLocation destination;
    @JsonProperty("close_source_branch")
    boolean closeSourceBranch;
}
