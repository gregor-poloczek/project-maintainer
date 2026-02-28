package io.github.gregorpoloczek.projectmaintainer.patching.service.patch;

import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.Workspace;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.WorkspaceService;
import io.github.gregorpoloczek.projectmaintainer.patching.common.EmptyTestPatch;
import io.github.gregorpoloczek.projectmaintainer.patching.common.IntegrationTestFileSystemProjectConnection;
import io.github.gregorpoloczek.projectmaintainer.patching.common.TestApplication;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchExecutionResult;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchService;
import io.github.gregorpoloczek.projectmaintainer.scm.service.git.GitService;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopyService;
import lombok.SneakyThrows;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
public class PatchServiceIntegrationTest {

    @TempDir
    static Path workspacesDirectory;

    @TempDir
    static Path remoteRepositoriesDirectory;

    @Autowired
    PatchService patchService;

    @Autowired
    WorkspaceService workspaceService;
    @Autowired
    private WorkingCopyService workingCopyService;

    @Autowired
    private GitService gitService;
    @Autowired
    private ProjectService projectService;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("project-maintainer.workspaces-directory", () -> workspacesDirectory.toString());
    }

    @AfterEach
    void tearDown() {
        for (Workspace workspace : workspaceService.findWorkspaces()) {
            workspaceService.deleteWorkspace(workspace);
        }

    }

    @Test
    void testPreviewWithEmptyPatch() {
        Path repository1 = this.createEmptyRemoteRepository("repository-1");

        Workspace workspace = workspaceService.createWorkspace(PatchServiceIntegrationTest.class.getSimpleName());

        workspaceService.updateConnections(workspace,
                List.of(IntegrationTestFileSystemProjectConnection.builder()
                        .id("connection-1")
                        .remoteRepository(repository1)
                        .build()));

        workspaceService.discoverProjects(workspace).blockLast();

        List<Project> projects = projectService.findAllByWorkspaceId(workspace.getId());
        assertThat(projects).hasSize(1);
        Project project = projects.getFirst();

        workingCopyService.cloneProject(project).blockLast();

        ProjectOperationProgress<PatchExecutionResult> progress =
                Objects.requireNonNull(patchService.previewPatch(project, EmptyTestPatch.ID, List.of()).blockLast());

        assertThat(progress.getState()).isEqualTo(OperationProgress.State.DONE);
    }

    @SneakyThrows({IOException.class, GitAPIException.class})
    private @NonNull Path createEmptyRemoteRepository(String name) {
        Path result = remoteRepositoriesDirectory.resolve(name);
        Files.createDirectory(result);

        try (Git git = Git.init()
                .setDirectory(result.toFile())
                .call()) {
            Path readme = result.resolve("README.md");
            Files.createFile(readme);

            git.add().addFilepattern(".").call();
            git.commit().setMessage("Initial commit").call();
            return result;
        }
    }
}
