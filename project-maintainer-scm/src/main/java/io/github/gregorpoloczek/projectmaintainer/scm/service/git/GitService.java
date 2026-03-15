package io.github.gregorpoloczek.projectmaintainer.scm.service.git;

import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress.State;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscovery;
import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscovery.PullRequestCreation;
import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.PullRequest;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.ProjectConnection;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.facets.GitUsernamePasswordCredentialsFacet;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.WorkspaceService;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.facets.BelongsToProjectConnection;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.util.stream.Collectors.toCollection;

@Service
@Slf4j
@RequiredArgsConstructor
public class GitService {

    private final List<ProjectDiscovery<?>> projectDiscoveries;
    private final WorkspaceService workspaceService;
    private final ProjectService projectService;

    public Flux<ProjectOperationProgress<PullResult>> pull(@NonNull WorkingCopy workingCopy) {

        return Flux.create(sink -> {
            sink.next(ProjectOperationProgress.<PullResult>builder()
                    .fqpn(workingCopy.getFQPN())
                    .state(State.SCHEDULED)
                    .message("Pulling ...")
                    .build());
            try {
                PullResult pullResult = workingCopy.withWriteLockAndThrowing(() -> {
                    final File directory = workingCopy.getDirectory();
                    // TODO [Working-Copy] configure transport not to not use credentials at the first attempt
                    try (Git git = Git.open(directory)) {
                        log.info("Pulling \"{}\".", directory);

                        var p = this.createGitActionContext(workingCopy, git)
                                .command(Git::pull)
                                .setProgressMonitor(new GitOperationProgressMonitor<>(sink, workingCopy.getFQPN()))
                                .call();

                        log.info("Pulled \"{}\" successfully.", directory);
                        return new PullResult(Commit.of((RevCommit) p.getMergeResult().getNewHead()));
                    }
                });
                sink.next(ProjectOperationProgress.<PullResult>builder()
                        .fqpn(workingCopy.getFQPN())
                        .state(State.DONE)
                        .progressCurrent(1)
                        .progressTotal(1)
                        .result(pullResult)
                        .build());
                sink.complete();

            } catch (Exception e) {
                log.error("Pulling failed.", e);
                sink.next(ProjectOperationProgress.<PullResult>builder()
                        .fqpn(workingCopy.getFQPN())
                        .throwable(e)
                        .state(State.FAILED)
                        .build());
                sink.error(e);
            }
        });
    }

    public Flux<ProjectOperationProgress<CloneResult>> clone(@NonNull final WorkingCopy workingCopy) {
        return Flux.<ProjectOperationProgress<CloneResult>>create(sink -> {
            sink.next(ProjectOperationProgress.<CloneResult>builder()
                    .fqpn(workingCopy.getFQPN())
                    .state(State.SCHEDULED)
                    .message("Cloning ...")
                    .build());
            CloneResult cloneResult = workingCopy.withWriteLock(() -> {
                final File directory = workingCopy.getDirectory();
                final URI uri = workingCopy.getURI();
                if (directory.exists()) {
                    log.error("Project \"{}\" has already been cloned", workingCopy.getFQPN());
                    throw new IllegalStateException("Project already cloned");
                }

                final CredentialsProvider credentialProvider = getCredentialsProvider(workingCopy);

                try {
                    log.info("Cloning \"{}\".", workingCopy.getFQPN());
                    Git.cloneRepository().setURI(uri.toString())
                            .setDirectory(directory)
                            .setCredentialsProvider(credentialProvider)
                            .setProgressMonitor(new GitOperationProgressMonitor<>(sink, workingCopy.getFQPN()))
                            .call().close();

                    final Optional<Commit> commit =
                            this.getLatestCommitHash(workingCopy);

                    final String currentBranch =
                            this.getCurrentBranch(workingCopy);

                    log.info("Cloned \"{}\" successfully.", workingCopy.getFQPN());
                    return new CloneResult(commit.orElse(null), currentBranch);
                } catch (GitAPIException e) {
                    log.error("Cloning failed.", e);
                    sink.next(ProjectOperationProgress.<CloneResult>builder()
                            .fqpn(workingCopy.getFQPN())
                            .throwable(e)
                            .state(State.FAILED)
                            .build());
                    throw new IllegalStateException(e);
                }
            });
            sink.next(ProjectOperationProgress.<CloneResult>builder()
                    .fqpn(workingCopy.getFQPN())
                    .state(OperationProgress.State.DONE)
                    .progressCurrent(1)
                    .progressTotal(1)
                    .result(cloneResult)
                    .build());
            sink.complete();
        }).onErrorResume(t -> Flux.just(ProjectOperationProgress.<CloneResult>builder()
                .fqpn(workingCopy.getFQPN())
                .state(State.FAILED)
                .throwable(t)
                .build()).concatWith(Mono.error(t)));
    }

    private Optional<Commit> getLatestCommitHash(@NonNull final WorkingCopy workingCopy) {
        try (Git git = Git.open(workingCopy.getDirectory())) {
            RevCommit latestCommit = git.
                    log().
                    setMaxCount(1).
                    call().
                    iterator().
                    next();

            return Optional.of(Commit.of(latestCommit));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoHeadException e) {
            return Optional.empty();
        } catch (GitAPIException e) {
            throw new IllegalStateException(e);
        }
    }

    private String getCurrentBranch(WorkingCopy workingCopy) {
        try (Git git = Git.open(workingCopy.getDirectory())) {
            return git.getRepository().getBranch();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    public Mono<List<PullRequest>> getOpenPullRequests(ProjectRelatable projectRelatable) {
        return getProjectDiscovery(projectRelatable).getOpenPullRequests(projectRelatable);
    }

    public Mono<PullRequest> createPullRequest(ProjectRelatable projectRelatable,
                                               PullRequestCreation pullRequestCreation) {
        return getProjectDiscovery(projectRelatable).createPullRequest(projectRelatable, pullRequestCreation);
    }

    private ProjectDiscovery<?> getProjectDiscovery(ProjectRelatable projectRelatable) {
        String type = this.projectService.require(projectRelatable).getFacet(BelongsToProjectConnection.class).get().getProjectConnection().getType();
        return projectDiscoveries.stream()
                .filter(pD -> pD.supports(type))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No project discovery found for %s".formatted(type)));
    }

    public Mono<Object> closePullRequest(ProjectRelatable projectRelatable, PullRequest pullRequest) {
        return getProjectDiscovery(projectRelatable).closePullRequest(projectRelatable, pullRequest);
    }

    public void resetAndStayInBranch(@NonNull WorkingCopy workingCopy) {
        // remove any changes
        this.execute(workingCopy, c -> {
            c.command(Git::reset)
                    .setMode(ResetCommand.ResetType.HARD)
                    .call();
            c.command(Git::clean)
                    .setCleanDirectories(true)
                    .call();

            Status status = c.command(Git::status)
                    .setIgnoreSubmodules(SubmoduleWalk.IgnoreSubmoduleMode.ALL)
                    .call();
            if (!status.isClean()) {
                throw new IllegalStateException("Working copy \"%s\" of \"%s\" was not properly reset".formatted(workingCopy, workingCopy.getFQPN()));
            }
        });
    }

    @FunctionalInterface
    public interface GitActionWithResult<T> {

        T execute(GitActionContext gitActionContext) throws GitAPIException;
    }

    @FunctionalInterface
    public interface GitActionWithoutResult {

        void execute(GitActionContext gitActionContext) throws GitAPIException, IOException;
    }

    public <T> T execute(WorkingCopy workingCopy, GitActionWithResult<T> action) {
        try (Git git = Git.open(workingCopy.getDirectory())) {
            return action.execute(createGitActionContext(workingCopy, git));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (GitAPIException e) {
            throw new IllegalStateException(e);
        }
    }

    public void execute(WorkingCopy workingCopy, GitActionWithoutResult action) {
        this.execute(workingCopy, git -> {
            try {
                action.execute(git);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return null;
        });
    }

    @SneakyThrows({GitAPIException.class})
    private BranchState getBranchState(GitActionContext gitActionContext) {
        gitActionContext.command(Git::fetch)
                .setRemoveDeletedRefs(true)
                .call();

        List<String> allBranches = gitActionContext.command(Git::branchList)
                .setListMode(ListMode.ALL)
                .call()
                .stream()
                .map(Ref::getName).toList();

        SortedSet<String> localBranches = allBranches.stream()
                .filter(name -> name.matches("^refs/heads/.+$"))
                .map(name -> name.replaceAll("^refs/heads/", ""))
                .collect(toCollection(TreeSet::new));
        SortedSet<String> remoteBranches = allBranches.stream()
                .filter(name -> name.matches("^refs/remotes/origin/.+$"))
                .map(name -> name.replaceAll("^refs/remotes/origin/", ""))
                .collect(toCollection(TreeSet::new));
        // TODO [Working-Copy] read default branch during project discovery
        String defaultBranch = localBranches.stream()
                .filter(b -> Set.of("master", "main").contains(b)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cannot find default branch"));

        return new BranchState(remoteBranches, localBranches, defaultBranch);
    }

    private GitActionContext createGitActionContext(WorkingCopy workingCopy, Git git) {
        CredentialsProvider credentialsProvider = getCredentialsProvider(workingCopy);
        Project project = projectService.require(workingCopy);

        GitActionContext tmp = GitActionContext.builder()
                .git(git)
                .project(project)
                .branchState(new BranchState(new TreeSet<>(), new TreeSet<>(), "undefined"))
                .credentialsProvider(credentialsProvider)
                .build();

        return GitActionContext.builder()
                .git(git)
                .project(project)
                .branchState(getBranchState(tmp))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    private CredentialsProvider getCredentialsProvider(WorkingCopy workingCopy) {
        ProjectConnection projectConnection = getProjectConnection(workingCopy);

        if (projectConnection.hasFacet(GitUsernamePasswordCredentialsFacet.class)) {
            GitUsernamePasswordCredentialsFacet upc = projectConnection.requireFacet(GitUsernamePasswordCredentialsFacet.class);
            return new UsernamePasswordCredentialsProvider(upc.getUsername(), upc.getPassword());
        } else {
            throw new IllegalStateException("No credentials available for connection " + projectConnection.getId());
        }
    }

    private ProjectConnection getProjectConnection(WorkingCopy workingCopy) {
        Project require = projectService.require(workingCopy);
        return this.workspaceService.requireConnection(require.getWorkspaceId(), require.getConnectionId());
    }


}
