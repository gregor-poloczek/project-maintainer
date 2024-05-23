package de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.bitbucket;

import de.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.common.ProjectDiscovery;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectDiscoveryContext;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class BitbucketProjectDiscovery implements ProjectDiscovery {

    private final ApplicationProperties applicationProperties;
    @Value("file:./.credentials/bitbucket.properties")
    private Resource credentials;


    public BitbucketProjectDiscovery(final ConversionService conversionService,
            ApplicationProperties applicationProperties) {
        this.conversionService = conversionService;
        this.applicationProperties = applicationProperties;
    }

    private final ConversionService conversionService;

    @Override
    public void discoverProjects(final ProjectDiscoveryContext context) {
        final Properties passwords;
        try {
            passwords = this.conversionService.convert(
                    this.credentials.getContentAsString(StandardCharsets.UTF_8),
                    Properties.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        for (String username : applicationProperties.getProjects().getDiscovery().bitbucket().users()) {
            String password = Optional.ofNullable(passwords.get(username)).map(String.class::cast)
                    .orElseThrow(() -> new IllegalStateException("Cannot find password for user " + username));
            String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
            WebClient client = WebClient.builder().baseUrl("https://api.bitbucket.org/2.0")
                    .defaultHeader("Authorization", "Basic " + auth)
                    .build();

            Mono<RepositoryListResource> response = client.get()
                    .uri("/repositories/" + username)
                    .retrieve()
                    .bodyToMono(RepositoryListResource.class);
            RepositoryListResource list = response.blockOptional().orElseThrow(IllegalStateException::new);
            for (RepositoryResource repository : list.values()) {
                context.discovered(c -> c.fqpn(FQPN.of("bitbucket", username, repository.name()))
                        .owner(username)
                        .uri(repository.links()
                                .klone()
                                .stream()
                                .filter(l -> l.name().equals("https"))
                                .findFirst()
                                .orElseThrow(IllegalStateException::new).href())
                        .credentials(new BitbucketCredentials(username, password))
                        .name(repository.name()));
            }
        }


    }
}
