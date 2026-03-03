package io.github.gregorpoloczek.projectmaintainer.patching.service.patch;

import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.Workspace;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.WorkspaceService;
import io.github.gregorpoloczek.projectmaintainer.patching.common.IntegrationTestFileSystemProjectDiscovery;
import io.github.gregorpoloczek.projectmaintainer.patching.common.TestPatchArguments;
import io.github.gregorpoloczek.projectmaintainer.patching.common.patches.MultipurposeTestPatch;
import io.github.gregorpoloczek.projectmaintainer.patching.common.patches.NoOpTestPatch;
import io.github.gregorpoloczek.projectmaintainer.patching.common.IntegrationTestFileSystemProjectConnection;
import io.github.gregorpoloczek.projectmaintainer.patching.common.TestApplication;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchExecutionResult;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchService;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.parameters.PatchParameterArgumentImpl;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchMetaData;
import io.github.gregorpoloczek.projectmaintainer.scm.service.git.GitService;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopyService;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
public class PatchServiceIntegrationTest {

    @Autowired
    ObjectProvider<TestPatchArguments> patchTestArgumentsProvider;

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

    @Autowired
    private IntegrationTestFileSystemProjectDiscovery integrationTestFileSystemProjectDiscovery;

    private Path repository1;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("project-maintainer.workspaces-directory", () -> workspacesDirectory.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.deleteDirectory(this.repository1.toFile());
        for (Workspace workspace : workspaceService.findWorkspaces()) {
            workspaceService.deleteWorkspace(workspace);
        }

        integrationTestFileSystemProjectDiscovery.reset();
    }

    @BeforeEach
    void setUp() {
        this.repository1 = this.createEmptyRemoteRepository("repository-1");
    }

    // TODO apply - branch already exists
    // TODO apply - pr already exists

    // TODO apply - add file
    // TODO apply - edit file
    // TODO apply - delete file

    // TODO stop - branch exists
    // TODO stop - branch + pr exists
    // TODO stop - branch does not exist

    // TODO branch name calculation (default + override)
    // TODO pr title calculation (default + override)

    @Test
    void testPreviewWithEmptyPatch() {
        Project project = createWorkspaceWithSingleRepository();

        // create a preview
        ProjectOperationProgress<PatchExecutionResult> previewProgress =
                Objects.requireNonNull(patchService.previewPatch(project, NoOpTestPatch.ID, List.of()).blockLast());

        assertThat(previewProgress.getState()).isEqualTo(OperationProgress.State.DONE);

        assertThat(previewProgress.getResult()
                .map(PatchExecutionResult::getDetail)
                .map(PatchExecutionResult.NoopResultDetail.class::cast)
        ).isPresent().get().satisfies(detail -> {
            assertThat(detail.getName()).isEqualTo("No-Op");
            assertThat(detail.getDescription()).isEqualTo("Patch did not change any files.");
        });
    }

    private TestPatchArguments arguments(String patchId) {
        return patchTestArgumentsProvider.getObject(patchId);
    }

    @Test
    void testPreviewWithFileManipulationPatch() {
        Project project = createWorkspaceWithSingleRepository();

        ProjectOperationProgress<PatchExecutionResult> previewProgress =
                Objects.requireNonNull(patchService.previewPatch(project, MultipurposeTestPatch.ID,
                                arguments(MultipurposeTestPatch.ID)
                                        .argument(MultipurposeTestPatch.Parameters.ADD_FILENAME, "file.txt")
                                        .argument(MultipurposeTestPatch.Parameters.EDIT_FILENAME, "two.txt")
                                        .argument(MultipurposeTestPatch.Parameters.DELETE_FILENAME, "three.txt"))
                        .blockLast());

        assertThat(previewProgress.getState()).isEqualTo(OperationProgress.State.DONE);

        PatchExecutionResult.PreviewGeneratedResultDetail detail =
                previewProgress.getResult()
                        .map(PatchExecutionResult::getDetail)
                        .map(PatchExecutionResult.PreviewGeneratedResultDetail.class::cast)
                        .orElseThrow();
        assertThat(detail.getName()).isEqualTo("Preview Generated");
        assertThat(detail.getDescription()).isEqualTo("Preview of all projected changes generated.");

        assertThat(detail.getUnifiedDiff())
                // added file
                .contains("--- /dev/null\n+++ file.txt\n@@ -0,0 +1,1 @@\n+My-Content")
                // edited file
                .contains("--- two.txt\n+++ two.txt\n@@ -1,1 +1,1 @@\n-# Two\n+My-Content")
                // deleted file
                .contains("--- three.txt\n+++ /dev/null\n@@ -1,1 +1,0 @@\n-# Three");
        // TODO [Patching] test operations
    }

    @Test
    void testApplyWithFileManipulationPatch() {
        Project project = createWorkspaceWithSingleRepository();

        ProjectOperationProgress<PatchExecutionResult> applyProgress =
                Objects.requireNonNull(patchService.applyPatch(project, MultipurposeTestPatch.ID,
                                arguments(MultipurposeTestPatch.ID)
                                        .argument(MultipurposeTestPatch.Parameters.ADD_FILENAME, "file.txt")
                                        .argument(MultipurposeTestPatch.Parameters.EDIT_FILENAME, "two.txt")
                                        .argument(MultipurposeTestPatch.Parameters.DELETE_FILENAME, "three.txt"))
                        .blockLast());

        assertThat(applyProgress.getState()).isEqualTo(OperationProgress.State.DONE);

        PatchExecutionResult.AppliedResultDetail detail =
                applyProgress.getResult()
                        .map(PatchExecutionResult::getDetail)
                        .map(PatchExecutionResult.AppliedResultDetail.class::cast)
                        .orElseThrow();
        assertThat(detail.getName()).isEqualTo("Patch applied");
        assertThat(detail.getDescription()).isEqualTo("All projected changes were applied in a remote branch, and a pull request was created.");

        assertThat(detail.getRemoteBranch().getName()).isEqualTo("project-maintainer/MultipurposeTestPatch");
        assertThat(detail.getPullRequest().getSourceBranchName()).isEqualTo("project-maintainer/MultipurposeTestPatch");
        assertThat(detail.getPullRequest().getTargetBranchName()).isEqualTo("master");
        assertThat(detail.getPullRequest().getTitle()).isEqualTo(MultipurposeTestPatch.ID);
        assertThat(detail.getCommitMessage()).isEqualTo("Applying patch \"MultipurposeTestPatch\": MultipurposeTestPatch");

        // TODO [Patching] ins remote repository gucken, ob alles geändert wurde
    }


    @Test
    void testApplyWithEmptyPatch() {
        Project project = createWorkspaceWithSingleRepository();

        // apply patch (which does nothing)
        ProjectOperationProgress<PatchExecutionResult> applyProgress =
                Objects.requireNonNull(patchService.applyPatch(project, NoOpTestPatch.ID, List.of()).blockLast());

        assertThat(applyProgress.getState()).isEqualTo(OperationProgress.State.DONE);

        assertThat(applyProgress.getResult()
                .map(PatchExecutionResult::getDetail)
                .map(PatchExecutionResult.NoopResultDetail.class::cast)
        ).isPresent().get().satisfies(detail -> {
            assertThat(detail.getName()).isEqualTo("No-Op");
            assertThat(detail.getDescription()).isEqualTo("Patch did not change any files.");
        });
    }

    private @NonNull Project createWorkspaceWithSingleRepository() {
        // create workspace and discover all projects
        Workspace workspace = workspaceService.createWorkspace(PatchServiceIntegrationTest.class.getSimpleName());

        workspace = workspaceService.updateConnections(workspace,
                List.of(IntegrationTestFileSystemProjectConnection.builder()
                        .remoteRepository(repository1)
                        .build()));

        OperationProgress<?> discoveryProgress =
                Objects.requireNonNull(workspaceService.discoverProjects(workspace).blockLast());
        assertThat(discoveryProgress.getState()).isEqualTo(OperationProgress.State.DONE);

        // determine project
        List<Project> projects = projectService.findAllByWorkspaceId(workspace.getId());
        assertThat(projects).hasSize(1);
        Project project = projects.getFirst();

        // attach project
        ProjectOperationProgress<Void> attachProgress =
                Objects.requireNonNull(workingCopyService.attachProject(project).blockLast());
        assertThat(attachProgress.getState()).isEqualTo(OperationProgress.State.DONE);
        return project;
    }

    @SneakyThrows({IOException.class, GitAPIException.class})
    private @NonNull Path createEmptyRemoteRepository(String name) {
        Path result = remoteRepositoriesDirectory.resolve(name);
        Files.createDirectory(result);

        try (Git git = Git.init()
                .setDirectory(result.toFile())
                .call()) {
            Path readme = result.resolve("README.md");
            createFile(readme, "# Some Project");

            createFile(result.resolve("one.txt"), "# One");
            createFile(result.resolve("two.txt"), "# Two");
            createFile(result.resolve("three.txt"), "# Three");

            git.add().addFilepattern(".").call();
            git.commit().setMessage("Initial commit").call();
            return result;
        }
    }

    @SneakyThrows({IOException.class})
    private static void createFile(Path result, String content) {
        Files.createFile(result);
        try (FileOutputStream fos = new FileOutputStream(result.toFile())) {
            IOUtils.write(content, fos, StandardCharsets.UTF_8);
        }
    }

    @Test
    void testGetAvailablePatches() {
        assertThat(
                patchService.getAvailablePatches().stream()
                        .map(PatchMetaData::getId).toList())
                .contains(MultipurposeTestPatch.ID, NoOpTestPatch.ID);
    }
}
