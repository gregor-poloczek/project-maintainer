package de.gregorpoloczek.projectmaintainer.core.domain.discovery.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.NotImplementedException;
import reactor.core.publisher.Mono;

public interface ProjectDiscovery {

    void discoverProjects(ProjectDiscoveryContext context);

    default Mono<Object> closePullRequest(ProjectRelatable projectRelatable, PullRequest pullRequest) {
        throw new NotImplementedException("Not implemented");
    }

    @Builder
    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Getter
    public static class PullRequestCreation {

        @NonNull
        String sourceBranchName;
        @NonNull
        String targetBranchName;
        @NonNull
        String title;
    }


    default Mono<PullRequest> createPullRequest(ProjectRelatable projectRelatable,
            PullRequestCreation pullRequestCreation) {
        throw new NotImplementedException("Not implemented");
    }

    default Mono<List<PullRequest>> getOpenPullRequests(ProjectRelatable projectRelatable) {
        throw new NotImplementedException("Not implemented");
    }

}
