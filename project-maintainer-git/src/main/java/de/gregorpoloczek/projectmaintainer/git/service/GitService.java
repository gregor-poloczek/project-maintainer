package de.gregorpoloczek.projectmaintainer.git.service;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationProgressListener;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GitService {

    public PullResult pull(@NonNull WorkingCopy workingCopy,
            @NonNull ProjectOperationProgressListener listener) {
        return workingCopy.withWriteLock(() -> {
            final File directory = workingCopy.getDirectory();
            try (Git git = Git.open(directory)) {
                log.info("Pulling \"{}\".", directory);

                final CredentialsProvider cP = workingCopy.getCredentialsProvider();

                var p = git.pull()
                        .setCredentialsProvider(cP)
                        .setProgressMonitor(new GitOperationProgressMonitor(listener))
                        .call();

                log.info("Pulling \"{}\" successfully.", directory);
                return new PullResult() {
                    @Override
                    public Optional<Commit> getLatestCommit() {
                        return Optional.of(CommitImpl.of((RevCommit) p.getMergeResult().getNewHead()));
                    }
                };
            } catch (IOException | GitAPIException e) {
                throw new ProjectPullFailedException(e);
            }
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

            return Optional.of(CommitImpl.of(latestCommit));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoHeadException e) {
            return Optional.empty();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    public CloneResult clone(@NonNull final WorkingCopy workingCopy,
            ProjectOperationProgressListener listener) {

        return workingCopy.withWriteLock(() -> {
            final File directory = workingCopy.getDirectory();
            final URI uri = workingCopy.getURI();
            if (directory.exists()) {
                log.error("Project \"{}\" has already been cloned", workingCopy.getFQPN());
                throw new ProjectAlreadyClonedException(workingCopy.getFQPN());
            }

            final CredentialsProvider credentialProvider = workingCopy.getCredentialsProvider();

            try {
                log.info("Cloning \"{}\".", workingCopy.getFQPN());
                Git.cloneRepository().setURI(uri.toString())
                        .setDirectory(directory)
                        .setCredentialsProvider(credentialProvider)
                        .setProgressMonitor(new GitOperationProgressMonitor(listener))
                        .call()
                        .close();
                final Optional<Commit> commit =
                        this.getLatestCommitHash(workingCopy);

                log.info("Cloned \"{}\" successfully.", workingCopy.getFQPN());
                return new CloneResult() {
                    @Override
                    public Optional<Commit> getLatestCommit() {
                        return commit;
                    }
                };
            } catch (GitAPIException e) {
                throw new RuntimeException(e);
            }
        });
    }


}
