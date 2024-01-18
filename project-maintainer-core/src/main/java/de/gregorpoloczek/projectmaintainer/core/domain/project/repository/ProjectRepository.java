package de.gregorpoloczek.projectmaintainer.core.domain.project.repository;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

import de.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import de.gregorpoloczek.projectmaintainer.core.domain.git.resolvers.common.ProjectDiscovery;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.Commit;
import de.gregorpoloczek.projectmaintainer.core.domain.git.service.GitService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.ProjectMetaData;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class ProjectRepository {

  private final File projectsDirectory;
  private final ApplicationProperties applicationProperties;
  private final GitService gitService;
  private final List<ProjectDiscovery> projectDiscoveries;
  private List<ProjectImpl> projects = new ArrayList<>();

  public ProjectRepository(final ApplicationProperties applicationProperties,
      final GitService gitService, List<ProjectDiscovery> projectDiscoveries) {
    this.projectsDirectory = applicationProperties.getProjects().getCloneDirectory();
    this.applicationProperties = applicationProperties;
    this.gitService = gitService;
    this.projectDiscoveries = projectDiscoveries;
  }

  @Async()
  public void init() {
    if (!projectsDirectory.exists()) {
      try {
        Files.createDirectories(projectsDirectory.toPath());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    final List<URI> configuredURIs = applicationProperties
        .getProjects()
        .getUris();

    final SortedSet<URI> discoveredURIs = new TreeSet<>();
    for (ProjectDiscovery projectDiscovery : projectDiscoveries) {
      discoveredURIs.addAll(projectDiscovery.getURIs());
    }

    final SortedSet<URI> uris = new TreeSet<>();
    uris.addAll(configuredURIs);
    uris.addAll(discoveredURIs);

    final List<ProjectImpl> projects = uris
        .stream()
        .map(this::toProject)
        .collect(toList());

    final SortedSet<FQPN> configuredProjects =
        projects.stream().map(Project::getFQPN)
            .collect(toCollection(TreeSet::new));

    final SortedSet<FQPN> existingProjects = this.findExistingProjects();
    projects.stream()
        .filter(p -> existingProjects.contains(p.getFQPN()))
        .forEach(p -> p.markAsCloned());

    for (ProjectImpl project : projects) {
      if (project.isCloned()) {
        final Optional<Commit> commit = this.gitService.getLatestCommitHash(project);
        commit.ifPresent(project::setLatestCommit);
      }
    }

    SortedSet<FQPN> projectsToRemove = new TreeSet<>();
    projectsToRemove.addAll(existingProjects);
    projectsToRemove.removeAll(configuredProjects);

    log.info("Removing {} obsolete projects", projectsToRemove.size());
    for (FQPN fqpn : projectsToRemove) {
      try {
        FileUtils.deleteDirectory(this.toDirectory(fqpn));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    this.projects = projects;
  }

  private ProjectImpl toProject(URI uri) {
    final ProjectMetaData metaData = this.gitService.toProjectMetaData(uri);
    return new ProjectImpl(toDirectory(metaData.getFQPN()), metaData);
  }

  private File toDirectory(final FQPN fqpn) {
    return projectsDirectory.toPath().resolve(fqpn.getValue().replaceAll("::", "/")).toFile();
  }

  private SortedSet<FQPN> findExistingProjects() {
    SortedSet<FQPN> existingClonedFQPNs = new TreeSet<>();

    try {
      Files.walkFileTree(projectsDirectory.toPath(), new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
            throws IOException {
          if (Files.exists(dir.resolve(".git"))) {
            existingClonedFQPNs.add(
                FQPN.of(projectsDirectory.toPath().relativize(dir).toString().replaceAll("/", "::")
                ));
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

  public List<ProjectImpl> findAll() {
    return this.projects;
  }

  public Optional<ProjectImpl> find(final FQPN fqpn) {
    return this.projects.stream().filter(p -> p.getFQPN().equals(fqpn)).findFirst();
  }
}
