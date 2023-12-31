package de.gregorpoloczek.projectmaintainer.core.git.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.CloneTarget;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.GitSource;
import de.gregorpoloczek.projectmaintainer.core.git.github.GithubCloneService;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.springframework.stereotype.Service;

@Service
public class GitCloneService {

  private final GithubCloneService githubCloneService;

  public GitCloneService(final GithubCloneService githubCloneService) {
    this.githubCloneService = githubCloneService;
  }

  public void clone(CloneTarget cloneTarget) {
    final Path projectsDirectory = Path.of("./.projects");
    final File directory = projectsDirectory.resolve(cloneTarget.getPath()).toFile();

    if (directory.exists()) {
      new ProjectAlreadyClonedException();
    }

    final CredentialsProvider credentialProvider = this.getCredentialProvider(cloneTarget);

    this.clone(cloneTarget.getUri(), directory, credentialProvider);
  }

  private CredentialsProvider getCredentialProvider(final CloneTarget cloneTarget) {
    if (cloneTarget.getGitSource() == GitSource.GITHUB) {
      return this.githubCloneService.getCredentialProvider();
    } else {
      throw new IllegalStateException(cloneTarget.getGitSource().name());
    }
  }

  public void clone(URI uri, File directory, CredentialsProvider credentialsProvider) {
    if (directory.exists()) {
      throw new ProjectAlreadyClonedException();
    }

    try {
      Git.cloneRepository().setURI(uri.toString())
          .setDirectory(directory)
          .setCredentialsProvider(credentialsProvider)
          .call();
    } catch (GitAPIException e) {
      throw new CloneFailedException(e);
    }
  }


}
