package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service;

import io.github.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.GenericOperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.DiscoveredProject;
import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscoveryResult;
import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscoveryService;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectImpl;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectMetaData;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRepository;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events.ProjectConnectionCreatedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events.ProjectConnectionDeletedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events.ProjectConnectionUpdatedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events.WorkspaceCreatedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events.WorkspaceDeletedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events.WorkspaceUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

    public static final String WORKSPACE_YML = "workspace.yml";
    private final List<WorkspaceImpl> workspaces = Collections.synchronizedList(new ArrayList<>());
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final ProjectDiscoveryService projectDiscoveryService;
    private final WorkspaceFileReader workspaceFileReader;
    private final WorkspaceFileWriter workspaceFileWriter;
    private final ApplicationProperties applicationProperties;

    private Path workspacesDirectory;
    private final ApplicationEventPublisher applicationEventPublisher;


    public ProjectConnection requireConnection(String workspaceId, String connectionId) {
        return this.requireWorkspace(workspaceId).getProjectConnections().stream().filter(connection -> connectionId.equals(connection.getId())).findFirst().orElseThrow(() -> new IllegalStateException("Connection with id " + connectionId + " not found"));
    }


    public List<Workspace> findWorkspaces() {
        return Collections.unmodifiableList(new ArrayList<>(this.workspaces));
    }

    public Optional<Workspace> findWorkspace(String id) {
        return workspaces.stream().filter(w -> w.id.equals(id))
                .map(Workspace.class::cast)
                .findFirst();
    }

    public Workspace requireWorkspace(String id) {
        return findWorkspace(id).orElseThrow(() -> new IllegalStateException("Workspace with id %s not found".formatted(id)));
    }


    public WorkspaceImpl requireWorkspaceInternal(String id) {
        return workspaces.stream().filter(w -> w.id.equals(id))
                .findFirst().orElseThrow();
    }


    String generateFreeWorkspaceId(String prefix) {
        String sanitizedPrefix = prefix
                .toLowerCase()
                .replaceAll("\\s", "-")
                .replaceAll("[^a-zA-Z0-9-]", "");
        long least = UUID.randomUUID().getLeastSignificantBits();
        String result;
        do {
            result = sanitizedPrefix + "-" + Long.toString(Math.abs(least), 36).substring(0, 8);
        } while (this.findWorkspace(result).isPresent());
        return result;
    }

    public Workspace updateConnections(Workspace workspace, List<? extends ProjectConnection> newConnections) {
        WorkspaceImpl ws = requireWorkspaceInternal(workspace.getId());

        List<ProjectConnection> oldConnections = ws.projectConnections;

        ws.projectConnections = List.copyOf(newConnections);

        log.info("Updated workspace {} ({} connections)", ws, newConnections.size());
        this.writeWorkspaceToFileSystem(ws);

        // notify workspace changes
        this.applicationEventPublisher.publishEvent(new WorkspaceUpdatedEvent(workspace));

        Set<String> existing = oldConnections.stream().map(ProjectConnection::getId).collect(Collectors.toSet());
        Set<String> saved = newConnections.stream().map(ProjectConnection::getId).collect(Collectors.toSet());
        Set<String> created = new HashSet<>(saved);
        created.removeAll(existing);
        Set<String> deleted = new HashSet<>(existing);
        deleted.removeAll(saved);
        Set<String> updated = new HashSet<>(existing);
        updated.removeAll(deleted);

        // notify connection changes
        newConnections.stream()
                .filter(c -> created.contains(c.getId()))
                .map(ProjectConnectionCreatedEvent::new)
                .forEach(applicationEventPublisher::publishEvent);
        newConnections.stream()
                .filter(c -> updated.contains(c.getId()))
                .map(ProjectConnectionUpdatedEvent::new)
                .forEach(applicationEventPublisher::publishEvent);
        oldConnections.stream()
                .filter(c -> deleted.contains(c.getId()))
                .map(ProjectConnectionDeletedEvent::new)
                .forEach(applicationEventPublisher::publishEvent);
        return ws;
    }

    public Workspace updateProjects(Workspace workspace, List<? extends Project> projects) {
        WorkspaceImpl ws = requireWorkspaceInternal(workspace.getId());
        ws.projects = List.copyOf(projects);

        this.writeWorkspaceToFileSystem(ws);

        this.applicationEventPublisher.publishEvent(new WorkspaceUpdatedEvent(workspace));

        return ws;
    }

    @SneakyThrows({IOException.class})
    public Workspace createWorkspace(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        boolean nameCollision = this.workspaces.stream().anyMatch(w -> w.getName().equals(name));
        if (nameCollision) {
            throw new IllegalArgumentException("Workspace already exists: " + name);
        }
        String workspaceId = generateFreeWorkspaceId(name);
        Path workspaceDirectory = workspacesDirectory.resolve(workspaceId);
        WorkspaceImpl workspace = WorkspaceImpl.builder()
                .id(workspaceId)
                .name(name)
                .directory(workspaceDirectory)
                .build();
        workspaces.add(workspace);

        if (Files.exists(workspaceDirectory)) {
            log.warn("Workspace already exists, will delete workspace directory: {}", workspaceDirectory);
            Files.delete(workspaceDirectory);
        }

        Files.createDirectories(toWorkspaceDirectory(workspace));

        this.writeWorkspaceToFileSystem(workspace);

        this.applicationEventPublisher.publishEvent(new WorkspaceCreatedEvent(workspace));

        return workspace;
    }

    @SneakyThrows({IOException.class})
    public void deleteWorkspace(Workspace workspace) {
        if (this.findWorkspace(workspace.getId()).isEmpty()) {
            throw new IllegalStateException("Workspace with id \"%s\" not found".formatted(workspace.getId()));
        }

        Path workspaceDirectory = toWorkspaceDirectory(workspace);
        if (Files.exists(workspaceDirectory)) {
            FileUtils.deleteDirectory(workspaceDirectory.toFile());
        }

        this.applicationEventPublisher.publishEvent(new WorkspaceDeletedEvent(workspace));

        this.workspaces.removeIf(w -> w.getId().equals(workspace.getId()));
        log.info("Deleted workspace {}", workspace);

    }

    private Path toWorkspaceDirectory(Workspace workspace) {
        return workspacesDirectory.resolve(workspace.getId());
    }


    @EventListener
    @SneakyThrows({IOException.class})
    public void on(ApplicationStartedEvent event) {
        this.workspacesDirectory = applicationProperties.getWorkspacesDirectory();
        if (!Files.exists(workspacesDirectory)) {
            log.info("Creating workspaces directory {}", workspacesDirectory);
            Files.createDirectories(workspacesDirectory);
        } else {
            log.info("Found workspaces directory {}", workspacesDirectory);
        }

        this.readWorkspacesFromFileSystem();
    }

    private void readWorkspacesFromFileSystem() throws IOException {
        try (Stream<Path> workspaceDirectories = Files
                .walk(workspacesDirectory, 1)
                .filter(d -> !d.equals(workspacesDirectory))
                .filter(Files::isDirectory)) {
            List<Path> workspaceFilePaths = workspaceDirectories.map(p -> p.resolve(WORKSPACE_YML)).toList();
            for (Path workspaceFilePath : workspaceFilePaths) {
                try {
                    WorkspaceFileReader.ReadResult r = this.workspaceFileReader.convertYamlFileToWorkspace(workspaceFilePath);
                    WorkspaceImpl workspace = r.getWorkspace();

                    log.info("Read workspace \"{}\" in {}", workspace.getId(), workspaceFilePath);
                    this.workspaces.add(workspace);
                    this.applicationEventPublisher.publishEvent(new WorkspaceCreatedEvent(workspace));

                    for (ProjectImpl project : r.getProjects()) {
                        this.projectService.save(project.getWorkspaceId(), project.getConnectionId(), project.getMetaData());
                    }

                } catch (InvalidWorkspaceFileException e) {
                    log.error("{} at {} was invalid. The workspace will be ignored.", WORKSPACE_YML, e.getWorkspaceFilePath(), e);
                }
            }
        }
    }

    @SneakyThrows({IOException.class})
    private void writeWorkspaceToFileSystem(WorkspaceImpl workspace) {
        Path workspaceDirectory = toWorkspaceDirectory(workspace);
        Path workspaceFile = workspaceDirectory.resolve(WORKSPACE_YML);

        workspaceFileWriter.write(workspace, workspaceFile);
    }


    public Flux<OperationProgress<Object>> discoverProjects(Workspace workspace) {
        return Flux.create(sink -> {
            // TODO [Workspaces] die sync von den patches wieder verwenden
            GenericOperationProgress.GenericOperationProgressBuilder<Object> progress =
                    GenericOperationProgress.builder()
                            .state(OperationProgress.State.RUNNING)
                            .result(new Object());

            log.info("Discovering projects for workspace {} using {} connections", workspace.getId(), workspace.getProjectConnections().size());

            // connections * (deletion + creation)
            progress.progressTotal(workspace.getProjectConnections().size() * 2);

            List<Project> finalProjects = new ArrayList<>();
            int current = 0;
            for (ProjectConnection projectConnection : workspace.getProjectConnections()) {
                sink.next(progress
                        .message("Loading from \"%s\"".formatted(projectConnection.getType()))
                        .progressCurrent(current++).build());

                // FQPN will receive a prefix, to distinguish the same project loaded via
                // different project connections
                FQPN prefix = FQPN.of(workspace.getId(), projectConnection.getId());

                ProjectDiscoveryResult projectDiscoveryResult = projectDiscoveryService.discoverProjects(projectConnection, prefix);
                List<DiscoveredProject> discovered = projectDiscoveryResult.getDiscoveredProjects();
                discovered.forEach(p -> {
                    log.info("* {}", p.getFQPN());
                    log.info("  Name: {}", p.getName());
                    p.getDescription().ifPresent(d -> log.info("  Description: {}", d));
                    log.info("  URI: {}", p.getURI());
                });
                List<FQPN> existing =
                        this.projectService.findAllByConnectionId(projectConnection.getId())
                                .stream().map(Project::getFQPN).toList();

                SortedSet<FQPN> projectsToRemove = new TreeSet<>();
                projectsToRemove.addAll(existing);
                projectsToRemove.removeAll(discovered.stream().map(DiscoveredProject::getFQPN).toList());

                sink.next(progress
                        .message("Removing obsolete projects for connection")
                        .progressCurrent(current++).build());

                log.info("Removing {} obsolete projects", projectsToRemove.size());
                for (FQPN fqpn : projectsToRemove) {
                    this.projectService.delete(fqpn);
                }

                sink.next(progress
                        .message("Saving newly discovered projects")
                        .progressCurrent(current++).build());
                for (DiscoveredProject discoveredProject : discovered) {
                    final FQPN fqpn = discoveredProject.getFQPN();

                    final ProjectMetaData metaData = ProjectMetaData.builder()
                            .fqpn(fqpn)
                            .description(discoveredProject.getDescription().orElse(null))
                            .name(discoveredProject.getName())
                            .uri(discoveredProject.getURI())
                            .owner(discoveredProject.getOwner())
                            .browserLink(discoveredProject.getBrowserLink().orElse(null))
                            .websiteLink(discoveredProject.getWebsiteLink().orElse(null))
                            .build();

                    Project project = this.projectService.save(workspace.getId(), projectConnection.getId(), metaData);

                    finalProjects.add(project);
                }
            }


            this.updateProjects(workspace, finalProjects);

            progress
                    .message("Finished project discovery")
                    .progressCurrent(current++)
                    .state(OperationProgress.State.DONE);
            sink.next(progress.build());
            sink.complete();
        });
    }

}
