package de.gregorpoloczek.projectmaintainer.core.git.github;

import de.gregorpoloczek.projectmaintainer.core.git.common.GitCloneService;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Path;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GithubCloneService {

  private final GitCloneService gitCloneService;

  @Value("${PROJECT_MAINTAINER_GITHUB_USERNAME}")
  private String username;
  @Value("${PROJECT_MAINTAINER_GITHUB_PASSWORD}")
  private String password;

  public GithubCloneService(final GitCloneService gitCloneService) {
    this.gitCloneService = gitCloneService;
  }

  @PostConstruct
  void init() {
    String userOrOrganization = "gregor-poloczek";
    String project = "project-maintainer";

    this.clone(userOrOrganization, project);
  }

  public void clone(final String userOrOrganization, final String project) {
    final File directory = Path.of("./", ".projects", "github", userOrOrganization, project)
        .toFile();

    final UsernamePasswordCredentialsProvider cp = new UsernamePasswordCredentialsProvider(
        username, password
    );

    this.gitCloneService.clone("https://github.com/%s/%s".formatted(userOrOrganization, project),
        directory, cp);
  }

}
