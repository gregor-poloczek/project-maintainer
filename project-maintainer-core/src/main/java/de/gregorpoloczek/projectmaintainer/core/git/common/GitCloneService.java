package de.gregorpoloczek.projectmaintainer.core.git.common;

import java.io.File;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.springframework.stereotype.Service;

@Service
public class GitCloneService {

  public void clone(String uri, File directory, CredentialsProvider credentialsProvider) {
    if (directory.exists()) {
      throw new ProjectAlreadyClonedException();
    }

    try {
      Git.cloneRepository().setURI(uri)
          .setDirectory(directory)
          .setCredentialsProvider(credentialsProvider)
          .call();
    } catch (GitAPIException e) {
      throw new CloneFailedException(e);
    }
  }


}
