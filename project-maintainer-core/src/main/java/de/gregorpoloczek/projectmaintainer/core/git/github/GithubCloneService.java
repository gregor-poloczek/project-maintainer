package de.gregorpoloczek.projectmaintainer.core.git.github;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GithubCloneService {

  @Value("${PROJECT_MAINTAINER_GITHUB_USERNAME}")
  private String username;
  @Value("${PROJECT_MAINTAINER_GITHUB_PASSWORD}")
  private String password;


  public CredentialsProvider getCredentialProvider() {
    final UsernamePasswordCredentialsProvider cp =
        new UsernamePasswordCredentialsProvider(username, password);
    return cp;
  }
}
