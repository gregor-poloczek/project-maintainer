package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.config.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.config.ProjectsFile;
import de.gregorpoloczek.projectmaintainer.core.git.common.GitService;
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
  private static final Pattern AWS_CODE_COMMIT = Pattern.compile(
      "^\\Qhttps://git-codecommit.\\E(?<region>[^.]+)\\Q.amazonaws.com\\E\\/v1\\/repos\\/(?<repository>.+)$");

  private final ApplicationProperties applicationProperties;

  private final GitService gitService;

  public ProjectService(final ApplicationProperties applicationProperties,
      final GitService gitService) {
    this.applicationProperties = applicationProperties;
    this.gitService = gitService;
  }

  @PostConstruct
  private void init() {
    this.cloneProjects();
  }

  @Autowired
  private ObjectMapper objectMapper;

  public CloneProjectsResult cloneProjects() {
    final File cloneDirectory = applicationProperties.getProjects().getCloneDirectory();
    final File projectsFileRaw = new File(cloneDirectory,
        "projects.json");

    ProjectsFile projectsFile;
    if (!projectsFileRaw.exists()) {
      if (cloneDirectory.exists()) {
        throw new IllegalStateException("Clone directory " + cloneDirectory
            + " already exists without a projects.file, cloning not possible.");
      }

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
      final File projectsDirectory = applicationProperties.getProjects().getCloneDirectory();
      final File directory = Path.of(projectsDirectory.toURI()).resolve(target.getPath())
          .toFile();

      final Project project = new Project(target.getUri().toString(), target.getFQPN());

      final boolean alreadyKnown = projectsFile.getProjects().stream()
          .anyMatch(p -> p.getFqpn().equals(target.getFQPN()));
      if (alreadyKnown) {
        gitService.pull(directory);
      } else {
        gitService.clone(target.getUri(), directory);
        projectsFile.getProjects().add(project);
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
    final Matcher awsCodeCommit = AWS_CODE_COMMIT.matcher(uri.toString());
    if (github.matches()) {
      final String owner = github.group("owner");
      final String repository = github.group("repository");
      return new CloneTarget(uri, Path.of("github", owner, repository));
    } else if (awsCodeCommit.matches()) {
      final String region = awsCodeCommit.group("region");
      final String repository = awsCodeCommit.group("repository");
      // TODO owner
      return new CloneTarget(uri, Path.of("aws-codecommit", region, repository));
    } else {
      throw new IllegalStateException(uri.toString());
    }
  }

}
