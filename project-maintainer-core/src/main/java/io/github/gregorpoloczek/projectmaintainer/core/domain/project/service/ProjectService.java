package io.github.gregorpoloczek.projectmaintainer.core.domain.project.service;

import java.util.List;
import java.util.Optional;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.events.ProjectCreatedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.events.ProjectDeletedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.events.ProjectUpdatedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.WorkspaceService;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events.ProjectConnectionDeletedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events.WorkspaceDeletedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.facets.BelongsToProjectConnection;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.facets.BelongsToWorkspace;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ProjectService {
    ApplicationEventPublisher eventPublisher;

    ProjectRepository projectRepository;
    ApplicationContext applicationContext;

    public Project save(String workspaceId, String connectionId, ProjectMetaData metaData) {
        ProjectImpl project = new ProjectImpl(workspaceId, connectionId, metaData);

        project.addFacet(BelongsToProjectConnection.class,
                BelongsToProjectConnection.of(() -> applicationContext.getBean(WorkspaceService.class).requireConnection(workspaceId, connectionId)));
        project.addFacet(BelongsToWorkspace.class,
                BelongsToWorkspace.of(() -> applicationContext.getBean(WorkspaceService.class).requireWorkspace(workspaceId)));

        boolean existing = projectRepository.find(project).isPresent();
        this.projectRepository.save(project, project);
        if (existing) {
            log.info("Project \"{}\" has been updated.", project.getFQPN());
            eventPublisher.publishEvent(new ProjectUpdatedEvent(project));
        } else {
            log.info("Project \"{}\" has been created.", project.getFQPN());
            eventPublisher.publishEvent(new ProjectCreatedEvent(project));
        }
        return project;
    }

    public List<Project> findAllByWorkspaceId(String workspaceId) {
        return this.projectRepository.findAll().stream()
                .filter(p -> p.getWorkspaceId().equals(workspaceId))
                .map(Project.class::cast)
                .toList();
    }

    public List<Project> findAllByConnectionId(String connectionId) {
        return this.projectRepository.findAll().stream()
                .filter(p -> p.getConnectionId().equals(connectionId))
                .map(Project.class::cast)
                .toList();
    }

    @EventListener
    public void on(WorkspaceDeletedEvent event) {
        this.findAllByWorkspaceId(event.getId())
                .forEach(p -> this.delete(p.getFQPN()));
    }

    @EventListener
    public void on(ProjectConnectionDeletedEvent event) {
        this.findAllByConnectionId(event.getId())
                .forEach(p -> this.delete(p.getFQPN()));
    }

    public List<Project> findAll() {
        return List.copyOf(this.projectRepository.findAll());
    }

    public Optional<Project> find(@NonNull final ProjectRelatable projectRelatable) {
        return this.projectRepository.find(projectRelatable).map(Project.class::cast);
    }

    public Project require(@NonNull final ProjectRelatable projectRelatable) {
        return this.projectRepository.require(projectRelatable);
    }

    public void delete(FQPN fqpn) {
        this.projectRepository.find(fqpn)
                .ifPresent(p -> {
                    log.info("Deleting project \"{}\".", fqpn);
                    this.eventPublisher.publishEvent(new ProjectDeletedEvent(p));
                    this.projectRepository.delete(p);
                    log.info("Project \"{}\" has been deleted.", fqpn);
                });
    }

}
