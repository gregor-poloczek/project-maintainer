package de.gregorpoloczek.projectmaintainer.git.service.discovery.provider.bitbucket;

import de.gregorpoloczek.projectmaintainer.git.ProjectsDiscoveryBitbucketProperties;
import de.gregorpoloczek.projectmaintainer.git.service.discovery.provider.bitbucket.api.RepositoryListResource;
import de.gregorpoloczek.projectmaintainer.git.service.discovery.provider.bitbucket.api.RepositoryResource;
import de.gregorpoloczek.projectmaintainer.git.service.discovery.provider.bitbucket.api.WorkspaceMembershipListResource;
import de.gregorpoloczek.projectmaintainer.git.service.discovery.provider.bitbucket.api.WorkspaceMembershipListResource.WorkspaceMembershipResource;
import de.gregorpoloczek.projectmaintainer.git.service.discovery.provider.common.PasswordResolverService;
import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscovery;
import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscoveryContext;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import java.net.URI;
import java.util.Base64;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class BitbucketProjectDiscovery implements ProjectDiscovery {

    private final PasswordResolverService passwordResolverService;
    private final ProjectsDiscoveryBitbucketProperties discoveryProperties;


    public BitbucketProjectDiscovery(
            final PasswordResolverService passwordResolverService,
            final ProjectsDiscoveryBitbucketProperties discoveryProperties) {
        this.passwordResolverService = passwordResolverService;
        this.discoveryProperties = discoveryProperties;
    }


    @Override
    public void discoverProjects(final ProjectDiscoveryContext context) {
        for (String username : discoveryProperties.getUsers()) {
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
                                    .description(repository.getDescription())
                                    .websiteLink(Optional.ofNullable(repository.getWebsite())
                                            .filter(StringUtils::isNotBlank)
                                            .orElse(null))
                                    .browserLink("https://bitbucket.org/%s/%s/src/master/".formatted(workspace,
                                            repository.getName()))
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
