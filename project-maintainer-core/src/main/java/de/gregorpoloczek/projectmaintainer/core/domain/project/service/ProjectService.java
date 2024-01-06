package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.projectsfile.ProjectJSON;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.projectsfile.ProjectsFileJSON;
import de.gregorpoloczek.projectmaintainer.core.git.common.GitService;
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
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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


  public Flux<CloneResult> cloneProjects() {
    return Flux.merge(this.projectRepository.findAll().stream()
        .map(p -> gitService.clone(p))
        .map(f -> Mono.fromFuture(f))
        .toList());

//    for (ProjectImpl project : this.projectRepository.findAll()) {
//      if (project.isCloned()) {
//        continue;
//      }
//      // TODO mutex
//      final CompletableFuture<Boolean> future = gitService.clone(project);
//      future.whenComplete((r, t) -> project.setCloned(t == null && r));
//    }

//    log.info("Pulling {} projects", projectsToPull.size());
//    for (FQPN fqpn : projectsToPull) {
//      gitService.pull(this.toDirectory(fqpn));
//    }
  }


}
