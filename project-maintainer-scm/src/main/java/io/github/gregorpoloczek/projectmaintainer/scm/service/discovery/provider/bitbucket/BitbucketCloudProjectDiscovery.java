package io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket;

import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.PullRequest;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.facets.BelongsToProjectConnection;
import io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.client.ApiClient;
import io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.client.api.V2PullRequestsApi;
import io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.client.api.V2RepositoriesApi;
import io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.client.api.V2WorkspacesApi;
import io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.client.model.V2Branch;
import io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.client.model.V2NamedLink;
import io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.client.model.V2NewPullRequest;
import io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.client.model.V2PaginatedPullRequests;
import io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.client.model.V2PaginatedRepositories;
import io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.client.model.V2PaginatedWorkspaceAccess;
import io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.client.model.V2PullRequest;
import io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.client.model.V2PullRequestEndpoint;
import io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.client.model.V2Repository;
import io.github.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.client.model.V2WorkspaceAccess;
import io.github.gregorpoloczek.projectmaintainer.scm.spi.bitbucket.BitbucketCloudRepositoryIdentifier;
import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscovery;
import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscoveryContext;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;

import java.net.URI;
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
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BitbucketCloudProjectDiscovery implements ProjectDiscovery<BitbucketCloudProjectConnection> {

    /**
     * Number of items requested per page for paginated calls (maximum supported by all used endpoints is 50).
     */
    private static final int PAGE_LENGTH = 50;

    ProjectService projectService;

    @Override
    public boolean supports(String type) {
        return type.equals(BitbucketCloudProjectConnection.TYPE);
    }

    @Override
    public void discoverProjects(final ProjectDiscoveryContext<BitbucketCloudProjectConnection> context) {
        BitbucketCloudProjectConnection connection = context.getConnection();
        ApiClient apiClient = createApiClient(connection);
        V2WorkspacesApi workspacesApi = new V2WorkspacesApi(apiClient);
        V2RepositoriesApi repositoriesApi = new V2RepositoriesApi(apiClient);

        List<V2WorkspaceAccess> memberships = getWorkspaces(workspacesApi);

        for (V2WorkspaceAccess membership : memberships) {
            String workspace = membership.getWorkspace().getSlug();

            // follow the pagination until there is no next page; on errors, the pages loaded so far are kept
            List<V2Repository> repositories = getRepositories(repositoriesApi, workspace);

            for (V2Repository repository : repositories) {

                Optional<String> maybeCloneLink = repository.getLinks()
                        .getClone()
                        .stream()
                        .filter(l -> l.getName().equals("https"))
                        .findFirst()
                        .map(V2NamedLink::getHref);
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
                        repository.getSlug());

                context.discovered(c -> c.fqpn(fqpn)
                        .owner(workspace)
                        .uri(URI.create(cloneLink))
                        .defaultBranch(Optional.ofNullable(repository.getMainbranch()).map(V2Branch::getName).orElse(null))
                        .description(repository.getDescription())
                        .websiteLink(Optional.ofNullable(repository.getWebsite())
                                .filter(StringUtils::isNotBlank)
                                .orElse(null))
                        .browserLink("https://bitbucket.org/%s/%s/src/%s/".formatted(
                                workspace, repository.getName(), repository.getMainbranch().getName()))
                        .name(repository.getName()));
            }
        }
    }

    private List<V2WorkspaceAccess> getWorkspaces(V2WorkspacesApi workspacesApi) {
        return workspacesApi.listWorkspacesForCurrentUser(1, PAGE_LENGTH)
                .expand(page -> page.getNext() != null
                        ? workspacesApi.listWorkspacesForCurrentUser(page.getPage() + 1, PAGE_LENGTH)
                        : Mono.empty())
                // can result in 403
                .doOnError(e -> {
                    // TODO [SCM] muss das sein?
                    if (e instanceof WebClientResponseException.Forbidden wcre) {
                        log.error("{}: {}", wcre.getMessage(), wcre.getResponseBodyAsString());
                    }
                })
                .flatMapIterable(V2PaginatedWorkspaceAccess::getValues)
                .collectList()
                .blockOptional().orElseThrow();
    }

    private List<V2Repository> getRepositories(V2RepositoriesApi repositoriesApi, String workspace) {
        return repositoriesApi.listRepositories(workspace, 1, PAGE_LENGTH)
                .expand(page -> page.getNext() != null
                        ? repositoriesApi.listRepositories(workspace, page.getPage() + 1, PAGE_LENGTH)
                        : Mono.empty())
                .flatMapIterable(V2PaginatedRepositories::getValues)
                .collectList()
                .blockOptional().orElseThrow();
    }

    private ApiClient createApiClient(ProjectRelatable projectRelatable) {
        BitbucketCloudProjectConnection projectConnection = this.projectService.require(projectRelatable).requireFacet(BelongsToProjectConnection.class).getProjectConnection();
        return createApiClient(projectConnection);
    }

    private ApiClient createApiClient(BitbucketCloudProjectConnection connection) {
        final int size = (int) DataSize.ofMegabytes(16).toBytes();
        WebClient webClient = ApiClient.buildWebClientBuilder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
                .build();

        ApiClient apiClient = new ApiClient(webClient);
        apiClient.setUsername(connection.getEmail());
        apiClient.setPassword(connection.getPassword());
        return apiClient;
    }

    @Override
    public Mono<Object> closePullRequest(ProjectRelatable projectRelatable, PullRequest pullRequest) {
        BitbucketCloudRepositoryIdentifier id = BitbucketCloudRepositoryIdentifier.of(projectRelatable);
        V2PullRequestsApi pullRequestsApi = new V2PullRequestsApi(createApiClient(id));

        return pullRequestsApi.declinePullRequest(id.getBitbucketWorkspace(), id.getBitbucketSlug(),
                        Integer.valueOf(pullRequest.getId().toString()))
                .cast(Object.class);
    }

    @Override
    public Mono<PullRequest> createPullRequest(ProjectRelatable projectRelatable, PullRequestCreation pullRequest) {
        BitbucketCloudRepositoryIdentifier id = BitbucketCloudRepositoryIdentifier.of(projectRelatable);
        V2PullRequestsApi pullRequestsApi = new V2PullRequestsApi(createApiClient(id));

        V2NewPullRequest body = new V2NewPullRequest()
                .title(pullRequest.getTitle())
                .source(new V2PullRequestEndpoint().branch(new V2Branch().name(pullRequest.getSourceBranchName())))
                .destination(new V2PullRequestEndpoint().branch(new V2Branch().name(pullRequest.getTargetBranchName())))
                .closeSourceBranch(true);

        return pullRequestsApi.createPullRequest(id.getBitbucketWorkspace(), id.getBitbucketSlug(), body)
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

    @Override
    public Mono<List<PullRequest>> getOpenPullRequests(ProjectRelatable projectRelatable) {
        return getAllPullRequests(BitbucketCloudRepositoryIdentifier.of(projectRelatable), "OPEN")
                .flatMapIterable(V2PaginatedPullRequests::getValues)
                .map(BitbucketCloudProjectDiscovery::convert)
                .map(PullRequest.class::cast)
                .collectList();
    }

    private Flux<V2PaginatedPullRequests> getAllPullRequests(BitbucketCloudRepositoryIdentifier id, String state) {
        V2PullRequestsApi pullRequestsApi = new V2PullRequestsApi(createApiClient(id));

        String workspace = id.getBitbucketWorkspace();
        String slug = id.getBitbucketSlug();

        return pullRequestsApi.listPullRequests(workspace, slug, state, 1, PAGE_LENGTH)
                .expand(page -> page.getNext() != null
                        ? pullRequestsApi.listPullRequests(workspace, slug, state, page.getPage() + 1, PAGE_LENGTH)
                        : Mono.empty());
    }

    private static PullRequestImpl convert(V2PullRequest prr) {
        return PullRequestImpl.builder()
                .id(prr.getId())
                .title(prr.getTitle())
                .targetBranchName(prr.getDestination().getBranch().getName())
                .sourceBranchName(prr.getSource().getBranch().getName())
                .browserLink(prr.getLinks().getHtml().getHref())
                .build();
    }
}
