package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.config.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.config.ProjectsFile;
import de.gregorpoloczek.projectmaintainer.core.git.common.GitCloneService;
import de.gregorpoloczek.projectmaintainer.core.git.common.ProjectAlreadyClonedException;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired
  private ObjectMapper objectMapper;

  public CloneProjectsResult cloneProjects() {
    final File projectsFileRaw = Path.of(".projects", "projects.json").toFile();

    ProjectsFile projectsFile;
    if (!projectsFileRaw.exists()) {
      projectsFile = new ProjectsFile();
      projectsFile.setVersion("1");
    } else {
      try {
        projectsFile = this.objectMapper.readValue(projectsFileRaw, ProjectsFile.class);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    final List<CloneTarget> targets = applicationProperties
        .getProjects().getUris()
        .stream()
        .map(this::toCloneTarget)
        .collect(toList());

    final CloneProjectsResultImpl result = new CloneProjectsResultImpl();
    for (CloneTarget target : targets) {
      try {
        gitCloneService.clone(target);
        projectsFile.getProjects().add(new Project(target.getFQPN(), target.getUri().toString()));
      } catch (ProjectAlreadyClonedException e) {
        log.info("Project \"{}\" already cloned ", target.getFQPN());
      }
    }

    try {
      IOUtils.write(this.objectMapper.writeValueAsString(projectsFile),
          new FileOutputStream(projectsFileRaw), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return result;
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
