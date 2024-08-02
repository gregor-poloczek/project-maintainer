package de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.github;

import de.gregorpoloczek.projectmaintainer.scm.ProjectsDiscoveryGithubProperties;
import de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.common.PasswordResolverService;
import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscovery;
import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscoveryContext;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedSearchIterable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GithubProjectDiscovery implements ProjectDiscovery {

    private final ProjectsDiscoveryGithubProperties discoveryProperties;

    public GithubProjectDiscovery(
            final PasswordResolverService passwordResolverService,
            ProjectsDiscoveryGithubProperties discoveryProperties) {
        this.passwordResolverService = passwordResolverService;
        this.discoveryProperties = discoveryProperties;
    }

    private final PasswordResolverService passwordResolverService;

    @Override
    public void discoverProjects(final ProjectDiscoveryContext context) {

        final List<String> users = discoveryProperties.getUsers();

        for (String username : users) {
            String password = passwordResolverService.getPassword("github", username);
            UsernamePasswordCredentialsProvider credentialsProvider =
                    new UsernamePasswordCredentialsProvider(username, password);

            try {
                GitHub github = new GitHubBuilder().withPassword(username, password).build();
                final PagedSearchIterable<GHRepository> list = github.searchRepositories()
                        .user(username).list();

                final List<GHRepository> repositories = list.toList();
                for (GHRepository repository : repositories) {
                    final URI uri = new URI(repository.getHttpTransportUrl());
                    context.discovered(b -> b.uri(uri)
                            .name(repository.getName())
                            .description(repository.getDescription())
                            .owner(repository.getOwnerName())
                            .browserLink(
                                    "https://github.com/%s/%s".formatted(username, repository.getName()))
                            .fqpn(FQPN.of("github", username, repository.getName()))
                            .credentialsProvider(credentialsProvider)
                    );
                }
            } catch (URISyntaxException | IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
