package de.gregorpoloczek.projectmaintainer.core.domain.discovery.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;

import java.util.List;

import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.NotImplementedException;
import reactor.core.publisher.Mono;

public interface ProjectDiscovery<T extends ProjectConnection> {

    boolean supports(String type);

    void discoverProjects(ProjectDiscoveryContext<T> context);

    default Mono<Object> closePullRequest(ProjectRelatable projectRelatable, PullRequest pullRequest) {
        throw new NotImplementedException("Pull request closing not implemented");
    }

    @Builder
    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Getter
    class PullRequestCreation {

        @NonNull
        String sourceBranchName;
        @NonNull
        String targetBranchName;
        @NonNull
        String title;
    }


    default Mono<PullRequest> createPullRequest(ProjectRelatable projectRelatable,
                                                PullRequestCreation pullRequestCreation) {
        throw new NotImplementedException("Pull request creation not implemented");
    }

    default Mono<List<PullRequest>> getOpenPullRequests(ProjectRelatable projectRelatable) {
        throw new NotImplementedException("Pull request loading not implemented");
    }

}
