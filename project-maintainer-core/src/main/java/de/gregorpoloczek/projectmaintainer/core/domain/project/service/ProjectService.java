package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.GitService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.repository.ProjectImpl;
import de.gregorpoloczek.projectmaintainer.core.domain.project.repository.ProjectRepository;
import de.gregorpoloczek.projectmaintainer.core.domain.project.repository.projectsfile.ProjectJSON;
import de.gregorpoloczek.projectmaintainer.core.domain.project.repository.projectsfile.ProjectsFileJSON;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
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
  private final File projectsDirectory;
  private final ProjectRepository projectRepository;
  private final File projectsFileRaw;

  public List<Project> getProjects() {
    return List.copyOf(this.projectRepository.findAll());
  }

  public ProjectService(
      final ApplicationProperties applicationProperties,
      final GitService gitService, final ObjectMapper objectMapper,
      final ProjectRepository projectRepository
  ) {
    this.applicationProperties = applicationProperties;
    this.gitService = gitService;
    this.objectMapper = objectMapper;
    this.projectsDirectory = applicationProperties.getProjects().getCloneDirectory();
    this.projectRepository = projectRepository;
    this.projectsFileRaw = new File(projectsDirectory, PROJECTS_FILE);
  }

  @PostConstruct
  private void postConstruct() {
    this.projectRepository.init();
  }

  private ProjectsFileJSON requireProjectsFile() {
    return this.readProjectsFile().orElseThrow(() -> new IllegalStateException(
        "Projects file not available, please clone projects firsts."));
  }

  private Optional<ProjectsFileJSON> readProjectsFile() {
    if (!this.projectsFileRaw.exists()) {
      return Optional.empty();
    }
    try {
      final ProjectsFileJSON file = this.objectMapper.readValue(projectsFileRaw,
          ProjectsFileJSON.class);
      Collections.sort(file.getProjects(), Comparator.comparing(ProjectJSON::getFQPN));
      return Optional.of(file);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void writeProjectsFile(ProjectsFileJSON projectsFile) {
    try {
      IOUtils.write(
          this.objectMapper.writer(new DefaultPrettyPrinter()).writeValueAsString(projectsFile),
          new FileOutputStream(projectsFileRaw), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void cloneProject(FQPN fqpn, ProjectOperationProgressListener listener) {
    this.gitService.clone(requireProject(fqpn), listener);
  }

  public void pullProject(FQPN fqpn, ProjectOperationProgressListener listener) {
    this.gitService.pull(requireProject(fqpn), listener);
  }

  private ProjectImpl requireProject(final FQPN fqpn) {
    return this.projectRepository.find(fqpn)
        .orElseThrow(() -> new ProjectNotFoundException(fqpn));
  }


  public void wipeProject(final FQPN fqpn, final ProjectOperationProgressListener emitter) {
    final ProjectImpl project = this.requireProject(fqpn);
    try {
      final File directory = project.getDirectory();
      FileUtils.deleteDirectory(directory);
      project.markAsNotCloned();
      emitter.succeeded(project);
    } catch (Exception e) {
      emitter.failed(project, e);
    }
  }
}
