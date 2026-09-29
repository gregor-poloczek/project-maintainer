package io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service;

import io.github.gregorpoloczek.projectmaintainer.core.common.TestApplication;
import io.github.gregorpoloczek.projectmaintainer.core.common.TestProjectConnection;
import io.github.gregorpoloczek.projectmaintainer.core.common.events.DomainObjectEvent;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.events.ProjectCreatedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.events.ProjectDeletedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.events.ProjectUpdatedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events.ProjectConnectionCreatedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events.ProjectConnectionDeletedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events.ProjectConnectionUpdatedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events.WorkspaceCreatedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events.WorkspaceDeletedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.events.WorkspaceUpdatedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.exceptions.ProjectConnectionNotFoundException;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.exceptions.WorkspaceAlreadyExistsException;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.exceptions.WorkspaceNameInvalidException;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.exceptions.WorkspaceNotFoundException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
@RecordApplicationEvents
class WorkspaceServiceIntegrationTest {

    @TempDir
    static Path workspacesDirectory;

    @Autowired
    WorkspaceService workspaceService;

    @Autowired
    ProjectService projectService;

    @Autowired
    ConfigurableApplicationContext applicationContext;

    @Autowired
    ApplicationEvents applicationEvents;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("project-maintainer.workspaces-directory", () -> workspacesDirectory.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        for (Workspace workspace : workspaceService.findWorkspaces()) {
            workspaceService.deleteWorkspace(workspace);
        }
        // remove leftovers not managed by the service (e.g. deliberately broken workspaces)
        FileUtils.cleanDirectory(workspacesDirectory.toFile());
    }

    @Test
    void createWorkspace_persistsWorkspaceAndPublishesEvent() {
        String name = "My Workspace!";
        Workspace workspace = workspaceService.createWorkspace(name);

        assertThat(workspace.getId()).matches("my-workspace-[a-z0-9]{8}");
        assertThat(workspace.getName()).isEqualTo(name);
        assertThat(workspace.getDirectory()).isEqualTo(workspacesDirectory.resolve(workspace.getId()));
        assertThat(workspace.getProjectConnections()).isEmpty();
        assertThat(workspace.getDirectory().resolve(WorkspaceService.WORKSPACE_YML)).isRegularFile();

        assertThat(workspaceService.findWorkspaces()).extracting(Workspace::getId).containsExactly(workspace.getId());
        assertThat(workspaceService.requireWorkspace(workspace.getId())).isSameAs(workspace);

        assertThat(idsOf(WorkspaceCreatedEvent.class)).containsExactly(workspace.getId());
    }

    @Test
    void createWorkspace_rejectsBlankName() {
        String name = " ";
        assertThatThrownBy(() -> workspaceService.createWorkspace(name))
                .isInstanceOfSatisfying(WorkspaceNameInvalidException.class,
                        e -> assertThat(e.getName()).isEqualTo(name));

        assertThat(workspaceService.findWorkspaces()).isEmpty();
    }

    @Test
    void createWorkspace_rejectsDuplicateName() {
        String name = "duplicate";
        workspaceService.createWorkspace(name);

        assertThatThrownBy(() -> workspaceService.createWorkspace(name))
                .isInstanceOfSatisfying(WorkspaceAlreadyExistsException.class,
                        e -> assertThat(e.getName()).isEqualTo(name));
        assertThat(workspaceService.findWorkspaces()).hasSize(1);
    }

    @Test
    void findAndRequireWorkspace_handleUnknownIds() {
        String name = "unknown";
        assertThat(workspaceService.findWorkspace(name)).isEmpty();
        assertThatThrownBy(() -> workspaceService.requireWorkspace(name))
                .isInstanceOfSatisfying(WorkspaceNotFoundException.class,
                        e -> assertThat(e.getId()).isEqualTo(name));
    }

    @Test
    void updateConnections_publishesCreatedUpdatedAndDeletedEvents() {
        Workspace workspace = workspaceService.createWorkspace("connections");
        // initial state: two connections, both of which are new to the workspace
        // - "kept" will survive the second update (with modified settings)
        // - "removed" will be missing from the second update
        TestProjectConnection kept = TestProjectConnection.builder().projectName("a").build();
        TestProjectConnection removed = TestProjectConnection.builder().build();
        workspaceService.updateConnections(workspace, List.of(kept, removed));

        // both connections are new, so each of them is announced as created
        assertThat(idsOf(ProjectConnectionCreatedEvent.class)).containsExactlyInAnyOrder(kept.getId(), removed.getId());
        // connections are stored as passed in, not copied
        assertThat(workspaceService.requireConnection(workspace.getId(), kept.getId())).isSameAs(kept);

        applicationEvents.clear();
        // second update covers all three kinds of changes at once:
        // - "keptModified" has the same id as "kept", but different settings -> updated
        // - "added" has a new id -> created
        // - "removed" is no longer part of the list -> deleted
        TestProjectConnection keptModified = kept.toBuilder().projectName("b").build();
        TestProjectConnection added = TestProjectConnection.builder().build();
        Workspace updated = workspaceService.updateConnections(workspace, List.of(keptModified, added));

        // the new list replaces the old one entirely, keeping its order
        assertThat(updated.getProjectConnections()).containsExactly(keptModified, added);
        // the workspace itself is announced as updated exactly once, regardless of the number of changed connections
        assertThat(idsOf(WorkspaceUpdatedEvent.class)).containsExactly(workspace.getId());
        // one event per connection, matching the kind of change (events of the first update were cleared above)
        assertThat(idsOf(ProjectConnectionCreatedEvent.class)).containsExactly(added.getId());
        assertThat(idsOf(ProjectConnectionUpdatedEvent.class)).containsExactly(kept.getId());
        assertThat(idsOf(ProjectConnectionDeletedEvent.class)).containsExactly(removed.getId());
        // the removed connection can no longer be resolved
        assertThatThrownBy(() -> workspaceService.requireConnection(workspace.getId(), removed.getId()))
                .isInstanceOfSatisfying(ProjectConnectionNotFoundException.class, e -> {
                    assertThat(e.getWorkspaceId()).isEqualTo(workspace.getId());
                    assertThat(e.getConnectionId()).isEqualTo(removed.getId());
                });
    }

    @Test
    void discoverProjects_savesDiscoveredProjectsAndRemovesObsoleteOnes() {
        Workspace workspace = workspaceService.createWorkspace("discovery");
        // discovering via this connection yields the two projects "project1" and "project2"
        TestProjectConnection connection = TestProjectConnection.builder()
                .projectName("project1")
                .projectName("project2")
                .build();
        workspace = workspaceService.updateConnections(workspace, List.of(connection));

        // discover projects
        List<OperationProgress<?>> progress = workspaceService.discoverProjects(workspace).collectList().block();
        assertThat(progress).isNotEmpty()
                .last().extracting(OperationProgress::getState).isEqualTo(OperationProgress.State.DONE);


        FQPN base = FQPN.of(workspace.getId(), connection.getId());
        FQPN project1 = base.append("project1");
        FQPN project2 = base.append("project2");
        assertThat(projectService.findAllByWorkspaceId(workspace.getId()))
                .extracting(Project::getFQPN)
                .containsExactlyInAnyOrder(project1, project2);
        // both projects are new, so each of them is announced as created
        assertThat(idsOf(ProjectCreatedEvent.class)).containsExactlyInAnyOrder(project1, project2);
        assertThat(idsOf(ProjectDeletedEvent.class)).isEmpty();

        Project savedProject1 = projectService.require(project1);
        assertThat(savedProject1.getConnectionId()).isEqualTo(connection.getId());
        assertThat(savedProject1.getMetaData().getName()).isEqualTo("project1");

        Project savedProject2 = projectService.require(project1);
        assertThat(savedProject2.getConnectionId()).isEqualTo(connection.getId());
        assertThat(savedProject2.getMetaData().getName()).isEqualTo("project1");

        // "project2" disappears at the source, and should therefore be removed on the next discovery
        workspace = workspaceService.updateConnections(workspace,
                List.of(connection.toBuilder().clearProjectNames().projectName("project1").build()));
        applicationEvents.clear();
        workspaceService.discoverProjects(workspace).blockLast();

        // "project1" is still there, "project2" has been deleted
        assertThat(projectService.findAllByWorkspaceId(workspace.getId()))
                .extracting(Project::getFQPN)
                .containsExactly(project1);
        // "project1" is re-saved and therefore announced as updated, not created
        assertThat(idsOf(ProjectCreatedEvent.class)).isEmpty();
        assertThat(idsOf(ProjectUpdatedEvent.class)).containsExactly(project1);
        assertThat(idsOf(ProjectDeletedEvent.class)).containsExactly(project2);

        assertThat(projectService.find(project1.getFQPN())).isNotEmpty();
        assertThat(projectService.find(project2.getFQPN())).isEmpty();
    }

    @Test
    void deleteWorkspace_removesDirectoryAndProjects() {
        Workspace workspace = workspaceService.createWorkspace("to-be-deleted");
        TestProjectConnection testProjectConnection = TestProjectConnection.builder().projectName("project1").build();
        workspace = workspaceService.updateConnections(workspace, List.of(testProjectConnection));

        workspaceService.discoverProjects(workspace).blockLast();
        assertThat(projectService.findAllByWorkspaceId(workspace.getId())).hasSize(1);

        workspaceService.deleteWorkspace(workspace);

        // workspace is deleted from disk
        assertThat(workspace.getDirectory()).doesNotExist();
        // workspace cannot be retrieved
        assertThat(workspaceService.findWorkspace(workspace.getId())).isEmpty();
        // projects cannot be found as well
        assertThat(projectService.findAllByWorkspaceId(workspace.getId())).isEmpty();

        // events were triggered
        assertThat(idsOf(WorkspaceDeletedEvent.class)).containsExactly(workspace.getId());
        assertThat(idsOf(ProjectDeletedEvent.class)).containsExactly(FQPN.of(workspace.getId(), testProjectConnection.getId(), "project1"));

        // cannot delete workspace twice
        Workspace alreadyDeleted = workspace;
        assertThatThrownBy(() -> workspaceService.deleteWorkspace(alreadyDeleted))
                .isInstanceOfSatisfying(WorkspaceNotFoundException.class,
                        e -> assertThat(e.getId()).isEqualTo(alreadyDeleted.getId()));
    }

    @Test
    void startup_restoresPersistedWorkspacesAndIgnoresInvalidOnes() throws IOException {
        String name = "persisted";

        Workspace workspace = workspaceService.createWorkspace(name);
        TestProjectConnection connection = TestProjectConnection.builder().projectName("project1").build();
        workspace = workspaceService.updateConnections(workspace, List.of(connection));
        workspaceService.discoverProjects(workspace).blockLast();

        Path folderWithoutWorkspaceFile = Files.createDirectories(workspacesDirectory.resolve("no-workspace-file"));

        Path folderWithInvalidWorkspaceFile = Files.createDirectories(workspacesDirectory.resolve("invalid-workspace-file"));
        Files.writeString(folderWithInvalidWorkspaceFile.resolve(WorkspaceService.WORKSPACE_YML), "foo: bar\n");

        simulateApplicationStart();

        assertThat(workspaceService.findWorkspaces()).hasSize(1);
        Workspace restored = workspaceService.requireWorkspace(workspace.getId());
        assertThat(restored).isNotSameAs(workspace);
        assertThat(restored.getName()).isEqualTo(name);
        assertThat(restored.getDirectory()).isEqualTo(workspace.getDirectory());
        assertThat(restored.getProjectConnections()).singleElement()
                .isInstanceOfSatisfying(TestProjectConnection.class, c -> {
                    assertThat(c.getId()).isEqualTo(connection.getId());
                    assertThat(c.getProjectNames()).containsExactly("project1");
                });

        // projects were found
        assertThat(projectService.findAllByWorkspaceId(restored.getId()))
                .extracting(Project::getFQPN).containsExactly(FQPN.of(workspace.getId(), connection.getId(), "project1"));

        // folder was not touched
        assertThat(folderWithoutWorkspaceFile).exists();

        // invalid workspace file was ignored, but neither deleted nor overwritten
        assertThat(folderWithInvalidWorkspaceFile.resolve(WorkspaceService.WORKSPACE_YML)).hasContent("foo: bar\n");
    }

    /**
     * Simulates an application start, which makes the service (re-)read all workspaces from the workspaces directory.
     */
    private void simulateApplicationStart() {
        workspaceService.on(new ApplicationStartedEvent(new SpringApplication(), new String[0], applicationContext, Duration.ZERO));
    }

    private List<Object> idsOf(Class<? extends DomainObjectEvent<?>> eventClass) {
        return applicationEvents.stream(eventClass)
                .<Object>map(DomainObjectEvent::getId)
                .toList();
    }
}
