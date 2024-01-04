package de.gregorpoloczek.projectmaintainer.core.domain.project.service;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.projectsfile.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.projectsfile.ProjectsFile;
import de.gregorpoloczek.projectmaintainer.core.git.common.CloneFailedException;
import de.gregorpoloczek.projectmaintainer.core.git.common.GitService;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
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
  private final File projectsFileRaw;

  public ProjectService(final ApplicationProperties applicationProperties,
      final GitService gitService, final ObjectMapper objectMapper) {
    this.applicationProperties = applicationProperties;
    this.gitService = gitService;
    this.objectMapper = objectMapper;
    this.projectsDirectory = applicationProperties.getProjects().getCloneDirectory();
    this.projectsFileRaw = new File(projectsDirectory, PROJECTS_FILE);
  }

  @PostConstruct
  private void init() {
    this.cloneProjects();
    this.listProjects();
  }

  public ListProjectsResult listProjects() {
    final ProjectsFile projectsFile = this.requireProjectsFile();

    for (Project project : projectsFile.getProjects()) {
      log.info("Found project \"{}\"", project.getFqpn());
    }

    return new ListProjectsResult() {
    };
  }

  private ProjectsFile requireProjectsFile() {
    return this.readProjectsFile().orElseThrow(() -> new IllegalStateException(
        "Projects file not available, please clone projects firsts."));
  }

  private Optional<ProjectsFile> readProjectsFile() {
    if (!this.projectsFileRaw.exists()) {
      return Optional.empty();
    }
    try {
      return Optional.of(this.objectMapper.readValue(projectsFileRaw, ProjectsFile.class));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void writeProjectsFile(ProjectsFile projectsFile) {
    try {
      IOUtils.write(
          this.objectMapper.writer(new DefaultPrettyPrinter()).writeValueAsString(projectsFile),
          new FileOutputStream(projectsFileRaw), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }


  public CloneProjectsResult cloneProjects() {
    if (!projectsDirectory.exists()) {
      try {
        Files.createDirectories(projectsDirectory.toPath());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    final ProjectsFile projectsFile = this.readProjectsFile()
        .orElseGet(() -> new ProjectsFile(SUPPORTED_VERSION));

    // TODO test SUPPORTED VERSION with another conversion type, otherwise there will be
    //  serialization problems

    final List<URI> uris = applicationProperties
        .getProjects()
        .getUris();

    final SortedSet<FQPN> existingClonedFQPNs = this.findExistingProjects(projectsDirectory);
    final Map<FQPN, URI> fqpnToUri = uris.stream()
        .collect(toMap(this.gitService::toFQPN, identity()));
    SortedSet<FQPN> configuredProjects = new TreeSet<>(fqpnToUri.keySet());

    final SortedSet<FQPN> projectsToClone = new TreeSet<>();
    projectsToClone.addAll(configuredProjects);
    projectsToClone.removeAll(existingClonedFQPNs);

    final SortedSet<FQPN> projectsToPull =
        configuredProjects.stream()
            .filter(existingClonedFQPNs::contains)
            .collect(toCollection(TreeSet::new));

    final SortedSet<FQPN> projectsToRemove =
        existingClonedFQPNs.stream().filter(p -> !configuredProjects.contains(p))
            .collect(toCollection(TreeSet::new));

    log.info("Cloning {} projects", projectsToClone.size());

    // TODO reset to default branch
    int failed = 0;
    for (FQPN fqpn : projectsToClone) {
      final File directory =
          projectsDirectory.toPath().resolve(fqpn.getValue()).toFile();
      final URI uri = fqpnToUri.get(fqpn);
      try {
        gitService.clone(uri, directory);
        final Project project = new Project(uri, fqpn);
        projectsFile.getProjects().add(project);
      } catch (CloneFailedException e) {
        failed++;
        log.error("Failed to clone " + uri, e);
      }
    }

    log.info("Pulling {} projects", projectsToPull.size());
    for (FQPN fqpn : projectsToPull) {
      final File directory =
          projectsDirectory.toPath().resolve(fqpn.getValue()).toFile();
      gitService.pull(directory);
    }

    log.info("Removing {} obsolete projects", projectsToRemove.size());
    for (FQPN fqpn : projectsToRemove) {
      final File directory =
          projectsDirectory.toPath().resolve(fqpn.getValue()).toFile();
      try {
        FileUtils.deleteDirectory(directory);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    projectsFile.getProjects().removeIf(p -> projectsToRemove.contains(p.getFqpn()));

    this.writeProjectsFile(projectsFile);
    if (failed > 0) {
      throw new IllegalStateException("Failed to clone %d projects.".formatted(failed));
    }

    return new CloneProjectsResult() {
    };
  }

  private SortedSet<FQPN> findExistingProjects(final File projectsDirectory) {
    SortedSet<FQPN> existingClonedFQPNs = new TreeSet<>();

    try {
      Files.walkFileTree(projectsDirectory.toPath(), new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
            throws IOException {
          if (Files.exists(dir.resolve(".git"))) {
            existingClonedFQPNs.add(
                FQPN.of(projectsDirectory.toPath().relativize(dir).toString()));
          }
          return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return existingClonedFQPNs;
  }


}
