package de.gregorpoloczek.projectmaintainer.core.git.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.CloneTarget;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GitCloneService {

  private final List<GitCredentialsProvider> credentialsProviders;

  public GitCloneService(
      final List<GitCredentialsProvider> credentialsProviders) {
    this.credentialsProviders = credentialsProviders;
  }

  public void clone(CloneTarget cloneTarget) {
    final Path projectsDirectory = Path.of("./.projects");
    final File directory = projectsDirectory.resolve(cloneTarget.getPath()).toFile();

    if (directory.exists()) {
      new ProjectAlreadyClonedException();
    }

    final CredentialsProvider credentialProvider = this.getCredentialsProvider(cloneTarget)
        .getCredentialsProvider();

    this.clone(cloneTarget.getUri(), directory, credentialProvider);
  }

  private GitCredentialsProvider getCredentialsProvider(final CloneTarget cloneTarget) {
    return this.credentialsProviders.stream().filter(cP -> cP.supports(cloneTarget.getUri()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "Could not find a git credentials provider for uri " + cloneTarget.getUri()));
  }

  private void clone(URI uri, File directory, CredentialsProvider credentialsProvider) {
    if (directory.exists()) {
      throw new ProjectAlreadyClonedException();
    }

    try {
      log.info("Cloning \"{}\".", uri);
      Git.cloneRepository().setURI(uri.toString())
          .setDirectory(directory)
          .setCredentialsProvider(credentialsProvider)
          .call();
      log.info("Cloned \"{}\" successfully.", uri);
    } catch (GitAPIException e) {
      throw new CloneFailedException(e);
    }
  }
}
