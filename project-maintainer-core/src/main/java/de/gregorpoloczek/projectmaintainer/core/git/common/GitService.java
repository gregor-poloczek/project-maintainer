package de.gregorpoloczek.projectmaintainer.core.git.common;

import de.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GitService {

  private final List<GitCredentialsProvider> credentialsProviders;
  private final ApplicationProperties applicationProperties;
  private final LogProgressMonitor logProgressMonitor;

  public GitService(
      final List<GitCredentialsProvider> credentialsProviders,
      final ApplicationProperties applicationProperties,
      final LogProgressMonitor logProgressMonitor) {
    this.credentialsProviders = credentialsProviders;
    this.applicationProperties = applicationProperties;
    this.logProgressMonitor = logProgressMonitor;
  }

  public void clone(URI uri, File directory) {

    if (directory.exists()) {
      new ProjectAlreadyClonedException();
    }

    final CredentialsProvider credentialProvider = this.getCredentialsProvider(uri)
        .getCredentialsProvider();

    if (directory.exists()) {
      throw new ProjectAlreadyClonedException();
    }

    try {
      log.info("Cloning \"{}\".", uri);
      Git.cloneRepository().setURI(uri.toString())
          .setDirectory(directory)
          .setCredentialsProvider(credentialProvider)
          .setProgressMonitor(this.logProgressMonitor)
          .call()
          .close();
      log.info("Cloned \"{}\" successfully.", uri);
    } catch (GitAPIException e) {
      throw new CloneFailedException(e);
    }
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
}
