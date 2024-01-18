package de.gregorpoloczek.projectmaintainer.core.domain.git.service;

import de.gregorpoloczek.projectmaintainer.core.domain.git.common.GitClonable;
import de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.common.GitProjectResolver;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationProgressListener;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.ProjectMetaData;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
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

  private final List<GitProjectResolver> gitProjectResolvers;

  public GitService(final List<GitProjectResolver> gitProjectResolvers) {
    this.gitProjectResolvers = gitProjectResolvers;
  }


  private GitProjectResolver getProjectResolver(final URI uri) {
    return this.gitProjectResolvers.stream()
        .filter(cP -> cP.supports(uri))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "Could not find a git credentials provider for uri " + uri));
  }

  public void pull(GitClonable project, ProjectOperationProgressListener listener) {
    project.withWriteLock(() -> {
      final File directory = project.getDirectory();
      try (Git git = Git.open(directory)) {
        log.info("Pulling \"{}\".", directory);

        final URI uri = project.getURI();
        final CredentialsProvider cP = this.getProjectResolver(uri)
            .getCredentialsProvider(uri);

        var p = git.pull()
            .setCredentialsProvider(cP)
            .setProgressMonitor(new GitOperationProgressMonitor(listener))
            .call();

        project.setLatestCommit(CommitImpl.of((RevCommit) p.getMergeResult().getNewHead()));

        listener.succeeded(project);
        log.info("Pulling \"{}\" successfully.", directory);
      } catch (IOException | GitAPIException e) {
        listener.failed(project, e);
        throw new PullFailedException(e);
      }
      return null;
    });
  }

  public ProjectMetaData toProjectMetaData(final URI uri) {
    return this.getProjectResolver(uri).getProjectMetaData(uri);
  }

  public Optional<Commit> getLatestCommitHash(final GitClonable project) {
    try (Git git = Git.open(project.getDirectory())) {
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

  public void clone(final GitClonable project, ProjectOperationProgressListener listener) {
    project.withWriteLock(() -> {
      final File directory = project.getDirectory();
      final URI uri = project.getMetaData().getURI();
      if (directory.exists()) {
        log.info("Project \"{}\" has already been cloned", project.getFQPN());
        listener.succeeded(project);
        return null;
      }

      final CredentialsProvider credentialProvider = this.getProjectResolver(uri)
          .getCredentialsProvider(project.getMetaData().getURI());

      try {
        log.info("Cloning \"{}\".", project.getFQPN());
        Git.cloneRepository().setURI(uri.toString())
            .setDirectory(directory)
            .setCredentialsProvider(credentialProvider)
            .setProgressMonitor(new GitOperationProgressMonitor(listener))
            .call()
            .close();
        final Optional<Commit> commit =
            this.getLatestCommitHash(project);
        project.setLatestCommit(commit.get());
        project.markAsCloned();

        listener.succeeded(project);
        log.info("Cloned \"{}\" successfully.", project.getFQPN());
      } catch (GitAPIException e) {
        listener.failed(project, e);
      }
      return null;
    });
  }
}
