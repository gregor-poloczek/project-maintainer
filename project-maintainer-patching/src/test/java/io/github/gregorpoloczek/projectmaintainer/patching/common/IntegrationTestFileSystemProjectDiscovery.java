package io.github.gregorpoloczek.projectmaintainer.patching.common;

import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscovery;
import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscoveryContext;
import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.PullRequest;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IntegrationTestFileSystemProjectDiscovery implements ProjectDiscovery<IntegrationTestFileSystemProjectConnection> {


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
        return Mono.just(List.of());
    }
}
