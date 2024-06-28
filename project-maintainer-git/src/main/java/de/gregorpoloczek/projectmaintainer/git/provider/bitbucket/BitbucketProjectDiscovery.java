package de.gregorpoloczek.projectmaintainer.git.provider.bitbucket;

import de.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import de.gregorpoloczek.projectmaintainer.core.common.properties.BitbucketDiscoverySection;
import de.gregorpoloczek.projectmaintainer.git.provider.bitbucket.api.RepositoryListResource;
import de.gregorpoloczek.projectmaintainer.git.provider.bitbucket.api.RepositoryResource;
import de.gregorpoloczek.projectmaintainer.git.provider.bitbucket.api.WorkspaceMembershipListResource;
import de.gregorpoloczek.projectmaintainer.git.provider.bitbucket.api.WorkspaceMembershipListResource.WorkspaceMembershipResource;
import de.gregorpoloczek.projectmaintainer.git.provider.common.PasswordResolverService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectDiscovery;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectDiscoveryContext;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.net.URI;
import java.util.Base64;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class BitbucketProjectDiscovery implements ProjectDiscovery {

    private final PasswordResolverService passwordResolverService;
    private final ApplicationProperties applicationProperties;


    public BitbucketProjectDiscovery(
            final PasswordResolverService passwordResolverService,
            final ApplicationProperties applicationProperties) {
        this.passwordResolverService = passwordResolverService;
        this.applicationProperties = applicationProperties;
    }


    @Override
    public void discoverProjects(final ProjectDiscoveryContext context) {
        BitbucketDiscoverySection bitbucketSection = applicationProperties.getProjects().getDiscovery().getBitbucket();
        if (bitbucketSection == null) {
            log.info("Nothing configured for Bitbucket.");
            return;
        }

        for (String username : bitbucketSection.getUsers()) {
            String password = passwordResolverService.getPassword("bitbucket", username);
            UsernamePasswordCredentialsProvider credentialsProvider =
                    new UsernamePasswordCredentialsProvider(username, password);

            String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
            WebClient client = WebClient.builder().baseUrl("https://api.bitbucket.org/2.0")
                    .defaultHeader("Authorization", "Basic " + auth)
                    .build();

            // can result in 403
            WorkspaceMembershipListResource membershipList = client.get()
                    .uri("/user/permissions/workspaces")
                    .retrieve()
                    .bodyToMono(WorkspaceMembershipListResource.class)
                    .blockOptional().orElseThrow(IllegalStateException::new);

            for (WorkspaceMembershipResource membership : membershipList.getValues()) {
                String workspace = membership.getWorkspace().getSlug();

                Integer nextPage = 1;
                do {
                    try {
                        Mono<RepositoryListResource> response = client.get()
                                .uri("/repositories/" + workspace + "?page=" + nextPage)
                                .retrieve()
                                .bodyToMono(RepositoryListResource.class);

                        RepositoryListResource list = response.blockOptional().orElseThrow(IllegalStateException::new);

                        for (RepositoryResource repository : list.getValues()) {
                            // TODO the same workspace could be used by two different users
                            // TODO workspace name necessary
                            context.discovered(c -> c.fqpn(FQPN.of("bitbucket",
                                            workspace,
                                            repository.getProject().getKey(),
                                            repository.getName()))
                                    .owner(workspace)
                                    .uri(URI.create(repository.getLinks()
                                            .getClone()
                                            .stream()
                                            .filter(l -> l.getName().equals("https"))
                                            .findFirst()
                                            .orElseThrow(IllegalStateException::new).getHref()))
                                    .credentialsProvider(credentialsProvider)
                                    .description(Optional.of(repository.getDescription()))
                                    .browserLink(Optional.of(
                                            "https://bitbucket.org/%s/%s/src/master/".formatted(workspace,
                                                    repository.getName())))
                                    .name(repository.getName()));
                        }
                        nextPage = list.getNext() != null ? list.getPage() + 1 : null;
                    } catch (WebClientResponseException e) {
                        log.error("Error listing bitbucket repositories", e);
                        break;
                    }
                } while (nextPage != null);
            }
        }


    }
}
