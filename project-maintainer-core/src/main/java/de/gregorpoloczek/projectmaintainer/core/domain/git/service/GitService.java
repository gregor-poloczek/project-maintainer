package de.gregorpoloczek.projectmaintainer.core.domain.git.service;

import de.gregorpoloczek.projectmaintainer.core.domain.git.GitClonable;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationProgressListener;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
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

  private final Map<GitClonable, Object> locks = Collections.synchronizedMap(new HashMap<>());
  private final List<GitCredentialsProvider> credentialsProviders;
  private final LogProgressMonitor logProgressMonitor;

  public GitService(
      final List<GitCredentialsProvider> credentialsProviders,
      final LogProgressMonitor logProgressMonitor) {
    this.credentialsProviders = credentialsProviders;
    this.logProgressMonitor = logProgressMonitor;
  }


  // TODO aspect dafür bauen
  private <T> T doWithProject(GitClonable project, Supplier<T> operation) {
    synchronized (this.locks.computeIfAbsent(project, (k) -> new Object())) {
      return operation.get();
    }
  }


  private GitCredentialsProvider getCredentialsProvider(final URI uri) {
    return this.credentialsProviders.stream()
        .filter(cP -> cP.supports(uri))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "Could not find a git credentials provider for uri " + uri));
  }

  public void pull(GitClonable project, ProjectOperationProgressListener listener) {
    this.doWithProject(project, () -> {
      final File directory = project.getDirectory();
      try (Git git = Git.open(directory)) {
        log.info("Pulling \"{}\".", directory);

        String remoteUrl = git.getRepository().getConfig().getString("remote", "origin", "url");
        final CredentialsProvider cP = this.getCredentialsProvider(new URI(remoteUrl))
            .getCredentialsProvider();

        var p = git.pull()
            .setCredentialsProvider(cP)
            .setProgressMonitor(new GitCloneProgressMonitor(listener))
            .call();

        project.setLatestCommit(toCommit((RevCommit) p.getMergeResult().getNewHead()));
        listener.succeeded();
        log.info("Pulling \"{}\" successfully.", directory);
      } catch (IOException | GitAPIException | URISyntaxException e) {
        listener.failed(e);
        throw new PullFailedException(e);
      }
      return null;
    });
  }

  public ProjectMetaData toProjectMetaData(final URI uri) {
    return this.getCredentialsProvider(uri).getProjectMetaData(uri);
  }

  public Optional<Commit> getLatestCommitHash(final GitClonable project) {
    try (Git git = Git.open(project.getDirectory())) {
      RevCommit latestCommit = git.
          log().
          setMaxCount(1).
          call().
          iterator().
          next();

      return Optional.of(toCommit(latestCommit));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (NoHeadException e) {
      return Optional.empty();
    } catch (GitAPIException e) {
      throw new RuntimeException(e);
    }
  }

  private Commit toCommit(final RevCommit latestCommit) {
    return new Commit() {
      @Override
      public Instant getTimestamp() {
        return Instant.ofEpochSecond(latestCommit.getCommitTime());
      }

      @Override
      public String getMessage() {
        return latestCommit.getFullMessage();
      }

      @Override
      public String getHash() {
        return latestCommit.getName();
      }
    };
  }

  public void clone(final GitClonable project, ProjectOperationProgressListener listener) {
    this.doWithProject(project, () -> {
      final File directory = project.getDirectory();
      final URI uri = project.getURI();
      if (directory.exists()) {
        listener.succeeded();
        return null;
      }

      final CredentialsProvider credentialProvider = this.getCredentialsProvider(uri)
          .getCredentialsProvider();

      try {
        log.info("Cloning \"{}\".", uri);
        Git.cloneRepository().setURI(uri.toString())
            .setDirectory(directory)
            .setCredentialsProvider(credentialProvider)
            .setProgressMonitor(new GitCloneProgressMonitor(listener))
            .call()
            .close();
        listener.succeeded();
        project.markAsCloned();
        final Optional<Commit> commit =
            this.getLatestCommitHash(project);
        project.setLatestCommit(commit.get());

        log.info("Cloned \"{}\" successfully.", uri);
      } catch (GitAPIException e) {
        listener.failed(e);
      }
      return null;
    });
  }
}
