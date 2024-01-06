package de.gregorpoloczek.projectmaintainer.core.git.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.CloneResult;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.GitClonable;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GitService {

  private final List<GitCredentialsProvider> credentialsProviders;
  private final LogProgressMonitor logProgressMonitor;

  public GitService(
      final List<GitCredentialsProvider> credentialsProviders,
      final LogProgressMonitor logProgressMonitor) {
    this.credentialsProviders = credentialsProviders;
    this.logProgressMonitor = logProgressMonitor;
  }

  @Async
  public CompletableFuture<CloneResult> clone(GitClonable project) {
    final File directory = project.getDirectory();
    final URI uri = project.getURI();
    if (directory.exists()) {
      return CompletableFuture.completedFuture(new CloneResult(project));
    }

    final CredentialsProvider credentialProvider = this.getCredentialsProvider(uri)
        .getCredentialsProvider();

    try {
      log.info("Cloning \"{}\".", uri);
      Git.cloneRepository().setURI(uri.toString())
          .setDirectory(directory)
          .setCredentialsProvider(credentialProvider)
          .setProgressMonitor(this.logProgressMonitor)
          .call()
          .close();
      project.markAsCloned();
      log.info("Cloned \"{}\" successfully.", uri);
    } catch (GitAPIException e) {
      throw new CloneFailedException(e);
    }
    return CompletableFuture.completedFuture(new CloneResult(project));
  }

  private GitCredentialsProvider getCredentialsProvider(final URI uri) {
    return this.credentialsProviders.stream()
        .filter(cP -> cP.supports(uri))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "Could not find a git credentials provider for uri " + uri));
  }

  public void pull(File directory) {
    try (Git git = Git.open(directory)) {
      log.info("Pulling \"{}\".", directory);

      String remoteUrl = git.getRepository().getConfig().getString("remote", "origin", "url");
      final CredentialsProvider cP = this.getCredentialsProvider(new URI(remoteUrl))
          .getCredentialsProvider();

      git.pull()
          .setCredentialsProvider(cP)
          .setProgressMonitor(this.logProgressMonitor)
          .call();
      log.info("Pulling \"{}\" successfully.", directory);
    } catch (IOException | GitAPIException | URISyntaxException e) {
      throw new PullFailedException(e);
    }
  }

  public FQPN toFQPN(final URI uri) {
    return this.getCredentialsProvider(uri).getFQPN(uri);
  }

  public Optional<Commit> getLatestCommitHash(final Project project) {
    try (Git git = Git.open(project.getDirectory())) {
      RevCommit latestCommit = git.
          log().
          setMaxCount(1).
          call().
          iterator().
          next();

      return Optional.of(new Commit() {
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
      });
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (NoHeadException e) {
      return Optional.empty();
    } catch (GitAPIException e) {
      throw new RuntimeException(e);
    }
  }
}
