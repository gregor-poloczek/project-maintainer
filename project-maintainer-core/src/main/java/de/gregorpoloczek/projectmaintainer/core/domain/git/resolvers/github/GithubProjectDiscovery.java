package de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.github;

import de.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.common.ProjectDiscovery;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectDiscoveryContext;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedSearchIterable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class GithubProjectDiscovery implements ProjectDiscovery {

    private final ApplicationProperties applicationProperties;
    @Value("file:./.credentials/github.properties")
    private Resource credentials;


    public GithubProjectDiscovery(final ConversionService conversionService,
            ApplicationProperties applicationProperties) {
        this.conversionService = conversionService;
        this.applicationProperties = applicationProperties;
    }

    private final ConversionService conversionService;

    @Override
    public void discoverProjects(final ProjectDiscoveryContext context) {
        final List<String> users = applicationProperties.getProjects().getDiscovery().github().users();
        final Properties passwords;
        try {
            passwords = this.conversionService.convert(
                    this.credentials.getContentAsString(StandardCharsets.UTF_8),
                    Properties.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        for (String username : users) {
            final String password =
                    Optional.ofNullable(passwords.getProperty(username))
                            .orElseThrow(() -> new IllegalStateException(
                                    "Cannot find password for username name %s".formatted(username)));
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
                            .fqpn(FQPN.of("github", username, repository.getName()))
                            .credentials(new GithubCredentials(username, password))
                    );
                }
            } catch (URISyntaxException | IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
