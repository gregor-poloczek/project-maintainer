package io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.github;

import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscovery;
import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscoveryContext;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedSearchIterable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GithubProjectDiscovery implements ProjectDiscovery<GithubProjectConnection> {

    @Override
    public boolean supports(String type) {
        return type.equals(GithubProjectConnection.TYPE);
    }

    @Override
    public void discoverProjects(final ProjectDiscoveryContext<GithubProjectConnection> context) {
        GithubProjectConnection connection = context.getConnection();

        try {
            GitHub github = new GitHubBuilder().withPassword(connection.getUsername(), connection.getPassword()).build();
            final PagedSearchIterable<GHRepository> list = github.searchRepositories()
                    .user(connection.getUsername()).list();

            final List<GHRepository> repositories = list.toList();
            for (GHRepository repository : repositories) {
                final URI uri = new URI(repository.getHttpTransportUrl());
                context.discovered(b -> b.uri(uri)
                        .name(repository.getName())
                        .description(repository.getDescription())
                        .owner(repository.getOwnerName())
                        .browserLink(repository.getHtmlUrl().toString())
                        .fqpn(FQPN.of(connection.getUsername(), repository.getName()))
                );
            }
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
