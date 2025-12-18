package de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket;

import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.PullRequest;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.workspace.service.facets.BelongsToProjectConnection;
import de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.api.PullRequestListResource;
import de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.api.PullRequestResource;
import de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.api.PullRequestResource.Branch;
import de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.api.PullRequestResource.PullRequestLocation;
import de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.api.RepositoryLinkResource;
import de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.api.RepositoryListResource;
import de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.api.RepositoryResource;
import de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.api.WorkspaceMembershipListResource;
import de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.api.WorkspaceMembershipListResource.WorkspaceMembershipResource;
import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscovery;
import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscoveryContext;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;

import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BitbucketCloudProjectDiscovery implements ProjectDiscovery<BitbucketCloudProjectConnection> {

    ProjectService projectService;

    @Override
    public boolean supports(String type) {
        return type.equals(BitbucketCloudProjectConnection.TYPE);
    }

    @Override
    public void discoverProjects(final ProjectDiscoveryContext<BitbucketCloudProjectConnection> context) {
        BitbucketCloudProjectConnection connection = context.getConnection();
        WebClient webClient = createWebClient(connection);

        // can result in 403
        WorkspaceMembershipListResource membershipList = webClient.get()
                .uri("/user/permissions/workspaces")
                .retrieve()
                .bodyToMono(WorkspaceMembershipListResource.class)
                .doOnError(e -> {
                    if (e instanceof WebClientResponseException.Forbidden wcre) {
                        log.error("{}: {}", wcre.getMessage(), wcre.getResponseBodyAsString());
                    }
                })
                .blockOptional().orElseThrow(IllegalStateException::new);

        for (WorkspaceMembershipResource membership : membershipList.getValues()) {
            String workspace = membership.getWorkspace().getSlug();

            Integer nextPage = 1;
            do {
                try {
                    Mono<RepositoryListResource> response = webClient.get()
                            .uri("/repositories/" + workspace + "?page=" + nextPage)
                            .retrieve()
                            .bodyToMono(RepositoryListResource.class);

                    RepositoryListResource list = response.blockOptional().orElseThrow(IllegalStateException::new);

                    for (RepositoryResource repository : list.getValues()) {

                        Optional<String> maybeCloneLink = repository.getLinks()
                                .getClone()
                                .stream()
                                .filter(l -> l.getName().equals("https"))
                                .findFirst()
                                .map(RepositoryLinkResource::getHref);
                        if (maybeCloneLink.isEmpty()) {
                            log.warn("Cannot determine clone link for repository {}/{}", workspace,
                                    repository.getName());
                            continue;
                        }
                        String cloneLink = maybeCloneLink.get();
                        String cloneUsername = cloneLink.replaceAll("^https://([^@]+)@.*$", "$1");
                        if (cloneUsername.contains("https")) {
                            log.warn("Cannot determine username for connecting to repository {}/{}", workspace,
                                    repository.getName());
                            continue;
                        }

                        FQPN fqpn = FQPN.of(
                                workspace,
                                repository.getProject().getKey(),
                                repository.getName());

                        context.discovered(c -> c.fqpn(fqpn)
                                .owner(workspace)
                                .uri(URI.create(cloneLink))
                                .description(repository.getDescription())
                                .websiteLink(Optional.ofNullable(repository.getWebsite())
                                        .filter(StringUtils::isNotBlank)
                                        .orElse(null))
                                .browserLink("https://bitbucket.org/%s/%s/src/%s/".formatted(
                                        workspace, repository.getName(), repository.getMainbranch().getName()))
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

    private WebClient createWebClient(ProjectRelatable projectRelatable) {
        BitbucketCloudProjectConnection projectConnection = this.projectService.require(projectRelatable).requireFacet(BelongsToProjectConnection.class).getProjectConnection();
        return createWebClient(projectConnection);
    }

    private WebClient createWebClient(BitbucketCloudProjectConnection connection) {
        String rawCredentials = connection.getEmail() + ":" + connection.getPassword();
        String encodedCredentials = Base64.getEncoder().encodeToString(rawCredentials.getBytes());

        final int size = (int) DataSize.ofMegabytes(16).toBytes();
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
                .build();
        return WebClient.builder().baseUrl("https://api.bitbucket.org/2.0")
                .exchangeStrategies(strategies)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials)
                .build();
    }

    @Override
    public Mono<Object> closePullRequest(ProjectRelatable projectRelatable, PullRequest pullRequest) {
        BitbucketRepositoryIds ids = getRepositoryIds(projectRelatable);

        WebClient webClient = createWebClient(projectRelatable);

        return webClient.post()
                .uri("/repositories/" + ids.getWorkspace() + "/" + ids.getSlug() + "/pullrequests/"
                        + pullRequest.getId().toString() + "/decline")
                .retrieve()
                .bodyToMono(Object.class);
    }

    @Override
    public Mono<PullRequest> createPullRequest(ProjectRelatable projectRelatable, PullRequestCreation pullRequest) {
        WebClient webClient = createWebClient(projectRelatable);

        BitbucketRepositoryIds ids = getRepositoryIds(projectRelatable);

        Branch source = Branch.builder().name(pullRequest.getSourceBranchName()).build();
        Branch destination = Branch.builder().name(pullRequest.getTargetBranchName()).build();
        PullRequestPostBodyResource body = PullRequestPostBodyResource.builder()
                .title(pullRequest.getTitle())
                .source(PullRequestLocation.builder().branch(source).build())
                .destination(PullRequestLocation.builder().branch(destination).build())
                // TODO testen, dass das hier wirklich funktioniert
                .closeSourceBranch(true)
                .build();

        return webClient.post()
                .uri("/repositories/" + ids.getWorkspace() + "/" + ids.getSlug() + "/pullrequests")
                .body(Mono.just(body), PullRequestPostBodyResource.class)
                .retrieve()
                .bodyToMono(PullRequestResource.class)
                .map(BitbucketCloudProjectDiscovery::convert);
    }


    @Getter
    @Builder
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class PullRequestImpl implements PullRequest {

        @NonNull
        Object id;
        @NonNull
        String title;
        @NonNull
        String sourceBranchName;
        @NonNull
        String targetBranchName;
        @NonNull
        String browserLink;
    }

    @Getter
    @RequiredArgsConstructor()
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static class BitbucketRepositoryIds {

        String workspace;
        String slug;
    }

    @Override
    public Mono<List<PullRequest>> getOpenPullRequests(ProjectRelatable projectRelatable) {
        // TODO repair
        WebClient webClient = createWebClient(projectRelatable);

        BitbucketRepositoryIds ids = getRepositoryIds(projectRelatable);

        // TODO pagination
        Mono<PullRequestListResource> response = webClient.get()
                .uri("/repositories/" + ids.getWorkspace() + "/" + ids.getSlug() + "/pullrequests?state=open")
                .retrieve()
                .bodyToMono(PullRequestListResource.class);

        return response
                .map(r -> r.getValues().stream()
                        .filter(pR -> pR.getState().equals("OPEN"))
                        .map(BitbucketCloudProjectDiscovery::convert).map(PullRequest.class::cast)
                        .toList());
    }

    private static PullRequestImpl convert(PullRequestResource prr) {
        return PullRequestImpl.builder()
                .id(prr.getId())
                .title(prr.getTitle())
                .targetBranchName(prr.getDestination().getBranch().getName())
                .sourceBranchName(prr.getSource().getBranch().getName())
                .browserLink(prr.getLinks().getHtml().getHref())
                .build();
    }

    private BitbucketRepositoryIds getRepositoryIds(ProjectRelatable projectRelatable) {
        // TODO ordentlicher identifizieren
        List<String> segments = projectRelatable.getFQPN().getSegments();
        String workspace = segments.get(2);
        String repositorySlug = segments.get(4);
        return new BitbucketRepositoryIds(workspace, repositorySlug);
    }
}
