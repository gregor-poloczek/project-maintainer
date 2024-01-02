package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.config.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.config.ProjectsFile;
import de.gregorpoloczek.projectmaintainer.core.git.common.CloneFailedException;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProjectService {

  public static final String PROJECTS_FILE = "projects.json";
  public static final String SUPPORTED_VERSION = "1";

  private final ApplicationProperties applicationProperties;
  private final GitService gitService;
  private final ObjectMapper objectMapper;

  public ProjectService(final ApplicationProperties applicationProperties,
      final GitService gitService, final ObjectMapper objectMapper) {
    this.applicationProperties = applicationProperties;
    this.gitService = gitService;
    this.objectMapper = objectMapper;
  }

  @PostConstruct
  private void init() {
    this.cloneProjects();
  }


  public CloneProjectsResult cloneProjects() {
    final File projectsDirectory = applicationProperties.getProjects().getCloneDirectory();
    final File projectsFileRaw =
        new File(projectsDirectory, PROJECTS_FILE);

    ProjectsFile projectsFile;
    if (!projectsFileRaw.exists()) {
      if (projectsDirectory.exists()) {
        throw new IllegalStateException(
            "Clone directory %s already exists without a %s, cloning not possible.".formatted(
                projectsDirectory, PROJECTS_FILE));
      }

      projectsFile = new ProjectsFile();
      projectsFile.setVersion(SUPPORTED_VERSION);
    } else {
      try {
        projectsFile = this.objectMapper.readValue(projectsFileRaw, ProjectsFile.class);
        if (!projectsFile.getVersion().equals(SUPPORTED_VERSION)) {
          throw new IllegalStateException(
              "Found projects file with unsupported version %s".formatted(
                  projectsFile.getVersion()));
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    final List<URI> uris = applicationProperties
        .getProjects()
        .getUris();

    final CloneProjectsResultImpl result = new CloneProjectsResultImpl();

    // TODO delete projects that no longer exist
    // TODO reset to default branch
    int failed = 0;
    for (URI uri : uris) {
      final String fqpn = gitService.toFQPN(uri);
      final boolean alreadyKnown = projectsFile.getProjects().stream()
          .anyMatch(p -> p.getFqpn().equals(fqpn));

      final File cloneDirectory = Path.of(projectsDirectory.toURI()).resolve(fqpn)
          .toFile();

      if (!alreadyKnown) {
        try {
          gitService.clone(uri, cloneDirectory);
          final Project project = new Project(uri.toString(), fqpn);
          projectsFile.getProjects().add(project);
        } catch (CloneFailedException e) {
          failed++;
          log.error("Failed to clone " + uri, e);
        }
      } else {
        gitService.pull(cloneDirectory);
      }
    }

    try {
      IOUtils.write(this.objectMapper.writeValueAsString(projectsFile),
          new FileOutputStream(projectsFileRaw), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    if (failed > 0) {
      throw new IllegalStateException("Failed to clone %d projects.".formatted(failed));
    }

    return result;
  }


}
