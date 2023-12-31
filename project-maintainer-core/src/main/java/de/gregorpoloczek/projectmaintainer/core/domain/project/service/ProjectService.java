package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import static java.util.stream.Collectors.toList;

import de.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import de.gregorpoloczek.projectmaintainer.core.git.common.GitCloneService;
import de.gregorpoloczek.projectmaintainer.core.git.common.ProjectAlreadyClonedException;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProjectService {

  public static final Pattern GITHUB_PATTERN = Pattern.compile(
      "^\\Qhttps://github.com/\\E(?<owner>[^/]+)/(?<repository>[^.]+)\\.git$");

  private final ApplicationProperties applicationProperties;

  private final GitCloneService gitCloneService;

  public ProjectService(final ApplicationProperties applicationProperties,
      final GitCloneService gitCloneService) {
    this.applicationProperties = applicationProperties;
    this.gitCloneService = gitCloneService;
  }

  @PostConstruct
  private void init() {
    this.cloneProjects();
    ;
  }

  public void cloneProjects() {
    final List<CloneTarget> targets = applicationProperties
        .getProjects().getUris()
        .stream()
        .map(this::toCloneTarget)
        .collect(toList());

    for (CloneTarget target : targets) {
      try {
        gitCloneService.clone(target);
      } catch (ProjectAlreadyClonedException e) {
        log.info("Project \"{}\" already cloned ", target.getFQPN());
      }
    }

  }

  private CloneTarget toCloneTarget(URI uri) {
    final Matcher github = GITHUB_PATTERN.matcher(uri.toString());
    if (github.matches()) {
      final String owner = github.group("owner");
      final String repository = github.group("repository");
      return new CloneTarget(GitSource.GITHUB, uri, Path.of("github", owner, repository));
    } else {
      throw new IllegalStateException(uri.toString());
    }
  }

}
