package de.gregorpoloczek.projectmaintainer.scm.service.git;

import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress.State;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscovery.PullRequestCreation;
import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.PullRequest;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import de.gregorpoloczek.projectmaintainer.scm.service.discovery.provider.bitbucket.BitbucketProjectDiscovery;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class GitService {

    private final BitbucketProjectDiscovery bitbucketProjectDiscovery;

    public Flux<ProjectOperationProgress<PullResult>> pull(@NonNull WorkingCopy workingCopy) {

        return Flux.create(sink -> {
            sink.next(ProjectOperationProgress.<PullResult>builder()
                    .fqpn(workingCopy.getFQPN())
                    .state(State.SCHEDULED)
                    .message("Pulling ...")
                    .build());
            PullResult pullResult = workingCopy.withWriteLock(() -> {

                final File directory = workingCopy.getDirectory();
                try (Git git = Git.open(directory)) {
                    log.info("Pulling \"{}\".", directory);

                    final CredentialsProvider cP = workingCopy.getCredentialsProvider();

                    var p = git.pull()
                            .setCredentialsProvider(cP)
                            .setProgressMonitor(new GitOperationProgressMonitor<>(sink, workingCopy.getFQPN()))
                            .call();

                    log.info("Pulling \"{}\" successfully.", directory);
                    return new PullResult(Commit.of((RevCommit) p.getMergeResult().getNewHead()));
                } catch (Exception e) {
                    log.error("Pulling failed.", e);
                    sink.next(ProjectOperationProgress.<PullResult>builder()
                            .fqpn(workingCopy.getFQPN())
                            .state(State.FAILED)
                            .build());
                    throw new IllegalStateException(e);
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

        });
    }


    public Flux<ProjectOperationProgress<CloneResult>> clone(@NonNull final WorkingCopy workingCopy) {
        return Flux.create(sink -> {
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

                final CredentialsProvider credentialProvider = workingCopy.getCredentialsProvider();

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
        });
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
        // TODO ordentlich abstrahieren
        if (projectRelatable.getFQPN().getFQPN().getSegments().getFirst().equals("bitbucket")) {
            return bitbucketProjectDiscovery.getOpenPullRequests(projectRelatable);
        }
        return Mono.just(Collections.emptyList());
    }

    public Mono<PullRequest> createPullRequest(ProjectRelatable projectRelatable,
            PullRequestCreation pullRequestCreation) {

        if (projectRelatable.getFQPN().getFQPN().getSegments().getFirst().equals("bitbucket")) {
            return bitbucketProjectDiscovery.createPullRequest(projectRelatable, pullRequestCreation);
        }
        return Mono.error(new IllegalStateException("Not implemented"));
    }

    public Mono<Object> closePullRequest(ProjectRelatable projectRelatable, PullRequest pullRequest) {
        if (projectRelatable.getFQPN().getFQPN().getSegments().getFirst().equals("bitbucket")) {
            return bitbucketProjectDiscovery.closePullRequest(projectRelatable, pullRequest);
        }
        return Mono.error(new IllegalStateException("Not implemented"));
    }

    @FunctionalInterface
    public interface GitActionWithResult<T> {

        T execute(Git git) throws GitAPIException;
    }

    @FunctionalInterface
    public interface GitActionWithoutResult {

        void execute(Git git) throws GitAPIException, IOException;
    }

    public <T> T execute(WorkingCopy workingCopy, GitActionWithResult<T> action) {
        try (Git git = Git.open(workingCopy.getDirectory())) {
            return action.execute(git);
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
}
