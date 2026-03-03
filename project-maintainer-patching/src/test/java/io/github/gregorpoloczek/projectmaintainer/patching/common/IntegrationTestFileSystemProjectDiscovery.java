package io.github.gregorpoloczek.projectmaintainer.patching.common;

import io.github.gregorpoloczek.projectmaintainer.core.common.repository.GenericProjectRelatableRepository;
import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscovery;
import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscoveryContext;
import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.PullRequest;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.BitbucketCloudProjectDiscovery;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IntegrationTestFileSystemProjectDiscovery implements ProjectDiscovery<IntegrationTestFileSystemProjectConnection> {
    AtomicInteger nextId = new AtomicInteger(1);

    private GenericProjectRelatableRepository<List<PullRequest>> pullRequests = new GenericProjectRelatableRepository<>();

    public void reset() {
        this.pullRequests.deleteAll();
    }

    @Override
    public boolean supports(String type) {
        return IntegrationTestFileSystemProjectConnection.TYPE.equals(type);
    }

    @Override
    public void discoverProjects(ProjectDiscoveryContext<IntegrationTestFileSystemProjectConnection> context) {

        IntegrationTestFileSystemProjectConnection connection = context.getConnection();
        List<Path> remoteRepositories = connection.getRemoteRepositories();

        for (Path remoteRepository : remoteRepositories) {
            String repositoryName = remoteRepository.getFileName().toString();
            context.discovered(b -> b.fqpn(FQPN.of(repositoryName)).name(repositoryName).uri(remoteRepository.toUri()).build());
        }
    }

    @Override
    public Mono<List<PullRequest>> getOpenPullRequests(ProjectRelatable projectRelatable) {
        return Mono.just(new ArrayList<>(this.pullRequests.find(projectRelatable).orElse(List.of())));
    }

    @Override
    public Mono<PullRequest> createPullRequest(ProjectRelatable projectRelatable, PullRequestCreation pullRequestCreation) {
        // TODO [Patching] prüfung, ob bereits ein entsprechender PR existiert?
        ArrayList<PullRequest> newValue =
                new ArrayList<>(this.pullRequests.find(projectRelatable).orElse(new ArrayList<>()));

        PullRequest pullRequest = BitbucketCloudProjectDiscovery.PullRequestImpl.builder().id(nextId.getAndIncrement())
                .title(pullRequestCreation.getTitle())
                .browserLink("https://www.google.de")
                .sourceBranchName(pullRequestCreation.getSourceBranchName())
                .targetBranchName(pullRequestCreation.getTargetBranchName()).build();
        newValue.add(pullRequest);
        this.pullRequests.save(projectRelatable, newValue);


        return Mono.just(pullRequest);
    }
}
