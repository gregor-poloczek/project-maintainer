package io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy;

import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationFailedException;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRepository;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.exceptions.ProjectNotFoundException;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.Workspace;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.WorkspaceService;
import io.github.gregorpoloczek.projectmaintainer.integrationtests.TestApplication;
import io.github.gregorpoloczek.projectmaintainer.integrationtests.support.IntegrationTestFileSystemProjectConnection;
import io.github.gregorpoloczek.projectmaintainer.integrationtests.support.IntegrationTestCleanupExtension;
import io.github.gregorpoloczek.projectmaintainer.scm.service.git.GitService;
import io.github.gregorpoloczek.projectmaintainer.scm.service.git.PullResult;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.exceptions.WorkingCopyNotFoundException;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.ArrayList;
import java.util.List;

import static io.github.gregorpoloczek.projectmaintainer.integrationtests.support.OperationProgressAssertions.requireDone;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
@RecordApplicationEvents
@ExtendWith(IntegrationTestCleanupExtension.class)
class WorkingCopyServiceIntegrationTest {

    private static final String DEFAULT_BRANCH = "master";
    private static final String README_MD = "README.md";

    @TempDir
    static Path workspacesDirectory;

    @TempDir
    static Path remoteRepositoriesDirectory;

    @Autowired
    WorkingCopyService workingCopyService;

    @Autowired
    WorkingCopyRepository workingCopyRepository;

    @Autowired
    GitService gitService;

    @Autowired
    WorkspaceService workspaceService;

    @Autowired
    ProjectService projectService;

    @Autowired
    ProjectRepository projectRepository;


    @Autowired
    ConfigurableApplicationContext applicationContext;

    @Autowired
    ApplicationEvents applicationEvents;

    private Path remoteRepository;
    private Workspace workspace;
    private Project project;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("project-maintainer.workspaces-directory", () -> workspacesDirectory.toString());
    }

    @BeforeEach
    void setUp() {
        this.remoteRepository = createRemoteRepository();

        // workspace with a single (discovered, but not yet attached) project
        this.workspace = workspaceService.createWorkspace(WorkingCopyServiceIntegrationTest.class.getSimpleName());
        this.workspace = workspaceService.updateConnections(workspace,
                List.of(IntegrationTestFileSystemProjectConnection.builder()
                        .remoteRepository(remoteRepository)
                        .build()));
        requireDone(workspaceService.discoverProjects(workspace));
        this.project = projectService.findAllByWorkspaceId(workspace.getId()).getFirst();
    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.deleteDirectory(remoteRepository.toFile());
    }

    @Test
    void attachProject_clonesRemoteRepository() {
        assertThat(workingCopyService.isAttached(project)).isFalse();

        requireDone(listener -> workingCopyService.attachProject(project, listener));

        WorkingCopy workingCopy = workingCopyService.require(project);
        assertThat(workingCopyService.isAttached(project)).isTrue();
        // the working copy is located in the workspace directory, one sub directory per FQPN segment
        assertThat(workingCopy.getDirectory().toPath()).isEqualTo(workspace.getDirectory()
                .resolve("working-copies")
                .resolve(String.join("/", project.getFQPN().getSegments())));
        assertThat(workingCopy.getDirectory().toPath().resolve(README_MD)).hasContent("# Some Project");
        assertThat(workingCopy.getCurrentBranch()).isEqualTo(DEFAULT_BRANCH);
        // commits are identified by their abbreviated hash
        assertThat(remoteHeadHash()).startsWith(workingCopy.getLatestCommit().orElseThrow().getHash());
    }

    @Test
    void require_failsForProjectWithoutWorkingCopy() {
        // the project has been discovered, but not attached
        assertThat(workingCopyService.find(project)).isEmpty();

        assertThatThrownBy(() -> workingCopyService.require(project))
                .isInstanceOfSatisfying(WorkingCopyNotFoundException.class,
                        e -> assertThat(e.getFQPN()).isEqualTo(project.getFQPN()));

        // operations requiring a working copy fail the same way
        assertThatThrownBy(() -> workingCopyService.pullProject(project, progress -> { }))
                .isInstanceOf(ProjectOperationFailedException.class)
                .hasCauseInstanceOf(WorkingCopyNotFoundException.class);
    }

    @Test
    void attachProjectBlocking_clonesRemoteRepositoryAndReportsProgress() {
        List<ProjectOperationProgress<Void>> progress = new ArrayList<>();

        WorkingCopy workingCopy = workingCopyService.attachProject(project, progress::add);

        assertThat(workingCopyService.isAttached(project)).isTrue();
        assertThat(workingCopy.getDirectory().toPath().resolve(README_MD)).hasContent("# Some Project");
        // the progress starts with SCHEDULED and ends with DONE, reported once for attaching as a whole
        assertThat(progress).extracting(ProjectOperationProgress::getState)
                .startsWith(OperationProgress.State.SCHEDULED)
                .endsWith(OperationProgress.State.DONE)
                .containsOnlyOnce(OperationProgress.State.DONE)
                .doesNotContain(OperationProgress.State.FAILED);
        assertThat(progress).extracting(ProjectOperationProgress::getFQPN).containsOnly(project.getFQPN());
    }

    @Test
    void attachProjectBlocking_reportsFailureExactlyOnce() {
        requireDone(listener -> workingCopyService.attachProject(project, listener));
        List<ProjectOperationProgress<Void>> progress = new ArrayList<>();

        // attaching an already attached project fails, as its working copy directory already exists
        assertThatThrownBy(() -> workingCopyService.attachProject(project, progress::add))
                .isInstanceOf(ProjectOperationFailedException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Project already cloned");

        // the failure is reported exactly once (and not again for cloning), carrying its cause
        assertThat(progress).extracting(ProjectOperationProgress::getState)
                .endsWith(OperationProgress.State.FAILED)
                .containsOnlyOnce(OperationProgress.State.FAILED)
                .doesNotContain(OperationProgress.State.DONE);
        assertThat(progress.getLast().getThrowable()).get()
                .satisfies(t -> assertThat(t).hasRootCauseInstanceOf(IllegalStateException.class));
    }

    @Test
    void pullBlocking_fetchesNewCommitsAndReportsProgress() {
        requireDone(listener -> workingCopyService.attachProject(project, listener));
        WorkingCopy workingCopy = workingCopyService.require(project);
        // the remote repository receives a new commit after the project has been attached
        String newCommitHash = commitInRemoteRepository("new.txt", "# New");
        List<ProjectOperationProgress<PullResult>> progress = new ArrayList<>();

        PullResult result = gitService.pull(workingCopy, progress::add);

        assertThat(workingCopy.getDirectory().toPath().resolve("new.txt")).hasContent("# New");
        // commits are identified by their abbreviated hash
        assertThat(newCommitHash).startsWith(result.getLatestCommit().orElseThrow().getHash());
        assertThat(progress).extracting(ProjectOperationProgress::getState)
                .startsWith(OperationProgress.State.SCHEDULED)
                .endsWith(OperationProgress.State.DONE)
                .containsOnlyOnce(OperationProgress.State.DONE)
                .doesNotContain(OperationProgress.State.FAILED);
        assertThat(progress.getLast().getResult()).contains(result);
    }

    @Test
    void pullBlocking_reportsFailureExactlyOnce() throws IOException {
        requireDone(listener -> workingCopyService.attachProject(project, listener));
        WorkingCopy workingCopy = workingCopyService.require(project);
        // the remote repository is gone, hence pulling fails
        FileUtils.deleteDirectory(remoteRepository.toFile());
        List<ProjectOperationProgress<PullResult>> progress = new ArrayList<>();

        assertThatThrownBy(() -> gitService.pull(workingCopy, progress::add))
                .isInstanceOfSatisfying(ProjectOperationFailedException.class,
                        e -> assertThat(e.getProgress()).isSameAs(progress.getLast()));

        // the failure is reported exactly once, carrying its cause
        assertThat(progress).extracting(ProjectOperationProgress::getState)
                .endsWith(OperationProgress.State.FAILED)
                .containsOnlyOnce(OperationProgress.State.FAILED)
                .doesNotContain(OperationProgress.State.DONE);
        assertThat(progress.getLast().getThrowable()).isPresent();
    }

    @Test
    void pullProjectBlocking_updatesWorkingCopyAndReportsProgress() {
        requireDone(listener -> workingCopyService.attachProject(project, listener));
        // the remote repository receives a new commit after the project has been attached
        String newCommitHash = commitInRemoteRepository("new.txt", "# New");
        List<ProjectOperationProgress<Void>> progress = new ArrayList<>();

        WorkingCopy workingCopy = workingCopyService.pullProject(project, progress::add);

        assertThat(workingCopy.getDirectory().toPath().resolve("new.txt")).hasContent("# New");
        // the stored working copy knows about the new commit
        assertThat(newCommitHash).startsWith(workingCopyService.require(project).getLatestCommit().orElseThrow().getHash());
        // the progress starts with SCHEDULED and ends with DONE, reported once for the operation as a whole
        assertThat(progress).extracting(ProjectOperationProgress::getState)
                .startsWith(OperationProgress.State.SCHEDULED)
                .endsWith(OperationProgress.State.DONE)
                .containsOnlyOnce(OperationProgress.State.DONE)
                .doesNotContain(OperationProgress.State.FAILED);
    }

    @Test
    void pullProjectBlocking_reportsFailureExactlyOnce() {
        // the project has not been attached, hence there is nothing to pull
        List<ProjectOperationProgress<Void>> progress = new ArrayList<>();

        assertThatThrownBy(() -> workingCopyService.pullProject(project, progress::add))
                .isInstanceOf(ProjectOperationFailedException.class)
                .hasCauseInstanceOf(WorkingCopyNotFoundException.class);

        assertThat(progress).extracting(ProjectOperationProgress::getState)
                .containsExactly(OperationProgress.State.FAILED);
        assertThat(progress.getLast().getThrowable()).get().isInstanceOf(WorkingCopyNotFoundException.class);
    }

    @Test
    void pullProject_fetchesNewCommits() {
        requireDone(listener -> workingCopyService.attachProject(project, listener));

        // the remote repository receives a new commit after the project has been attached
        String newCommitHash = commitInRemoteRepository("new.txt", "# New");

        requireDone(listener -> workingCopyService.pullProject(project, listener));

        WorkingCopy workingCopy = workingCopyService.require(project);
        assertThat(workingCopy.getDirectory().toPath().resolve("new.txt")).hasContent("# New");
        assertThat(newCommitHash).startsWith(workingCopy.getLatestCommit().orElseThrow().getHash());
    }

    @Test
    void detachProjectBlocking_removesWorkingCopyAndReportsProgress() {
        requireDone(listener -> workingCopyService.attachProject(project, listener));
        Path directory = workingCopyService.require(project).getDirectory().toPath();
        List<ProjectOperationProgress<Void>> progress = new ArrayList<>();

        workingCopyService.detachProject(project, progress::add);

        assertThat(workingCopyService.isAttached(project)).isFalse();
        assertThat(directory).doesNotExist();
        assertThat(applicationEvents.stream(ProjectDetachedEvent.class))
                .extracting(ProjectDetachedEvent::getFQPN)
                .containsExactly(project.getFQPN());
        assertThat(progress).extracting(ProjectOperationProgress::getState)
                .containsExactly(OperationProgress.State.SCHEDULED, OperationProgress.State.DONE);
    }

    @Test
    void detachProjectBlocking_reportsFailureExactlyOnce() {
        // an unknown project cannot be detached
        FQPN unknown = FQPN.of("unknown-workspace", "unknown-connection", "unknown-project");
        List<ProjectOperationProgress<Void>> progress = new ArrayList<>();

        assertThatThrownBy(() -> workingCopyService.detachProject(unknown, progress::add))
                .isInstanceOf(ProjectOperationFailedException.class)
                .hasCauseInstanceOf(ProjectNotFoundException.class);

        assertThat(progress).extracting(ProjectOperationProgress::getState)
                .containsExactly(OperationProgress.State.SCHEDULED, OperationProgress.State.FAILED);
        assertThat(applicationEvents.stream(ProjectDetachedEvent.class)).isEmpty();
    }

    @Test
    void detachProject_removesWorkingCopy() {
        requireDone(listener -> workingCopyService.attachProject(project, listener));
        Path directory = workingCopyService.require(project).getDirectory().toPath();

        requireDone(listener -> workingCopyService.detachProject(project, listener));

        assertThat(workingCopyService.isAttached(project)).isFalse();
        assertThat(directory).doesNotExist();
        assertThat(applicationEvents.stream(ProjectDetachedEvent.class))
                .extracting(ProjectDetachedEvent::getFQPN)
                .containsExactly(project.getFQPN());
        // the project itself remains
        assertThat(projectService.find(project)).isPresent();
    }

    @Test
    void deletingProject_removesWorkingCopy() {
        requireDone(listener -> workingCopyService.attachProject(project, listener));
        Path directory = workingCopyService.require(project).getDirectory().toPath();

        // removing the connection removes its projects, and hence their working copies
        workspaceService.updateConnections(workspace, List.of());

        assertThat(projectService.find(project)).isEmpty();
        assertThat(workingCopyService.isAttached(project)).isFalse();
        assertThat(directory).doesNotExist();
    }

    @Test
    void restart_restoresExistingWorkingCopy() {
        requireDone(listener -> workingCopyService.attachProject(project, listener));
        WorkingCopy attached = workingCopyService.require(project);

        // simulate a restart: projects and working copies are only kept in memory, the working copy directory remains
        projectRepository.deleteAll();
        workingCopyRepository.deleteAll();
        workspaceService.on(new ApplicationStartedEvent(new SpringApplication(), new String[0], applicationContext, Duration.ZERO));

        // re-reading the workspace re-creates the project, which picks up the existing working copy without cloning
        WorkingCopy restored = workingCopyService.require(project);
        assertThat(restored).isNotSameAs(attached);
        assertThat(restored.getDirectory()).isEqualTo(attached.getDirectory());
        assertThat(restored.getCurrentBranch()).isEqualTo(DEFAULT_BRANCH);
        assertThat(remoteHeadHash()).startsWith(restored.getLatestCommit().orElseThrow().getHash());
    }

    @Test
    @SneakyThrows({IOException.class, GitAPIException.class})
    void resetAndCheckoutDefaultBranch_discardsChangesAndReturnsToDefaultBranch() {
        requireDone(listener -> workingCopyService.attachProject(project, listener));
        WorkingCopy workingCopy = workingCopyService.require(project);
        Path directory = workingCopy.getDirectory().toPath();

        // leave the working copy in a dirty state on another branch
        try (Git git = Git.open(directory.toFile())) {
            git.checkout().setCreateBranch(true).setName("feature").call();
        }
        Files.writeString(directory.resolve(README_MD), "changed");
        Files.writeString(directory.resolve("untracked.txt"), "untracked");

        workingCopyService.resetAndCheckoutDefaultBranch(workingCopy);

        try (Git git = Git.open(directory.toFile())) {
            assertThat(git.getRepository().getBranch()).isEqualTo(DEFAULT_BRANCH);
        }
        assertThat(directory.resolve(README_MD)).hasContent("# Some Project");
        assertThat(directory.resolve("untracked.txt")).doesNotExist();
    }

    @SneakyThrows({IOException.class, GitAPIException.class})
    private Path createRemoteRepository() {
        Path result = Files.createDirectory(remoteRepositoriesDirectory.resolve("repository-1"));
        try (Git git = Git.init().setInitialBranch(DEFAULT_BRANCH).setDirectory(result.toFile()).call()) {
            Files.writeString(result.resolve(README_MD), "# Some Project");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Initial commit").call();
        }
        return result;
    }

    @SneakyThrows({IOException.class, GitAPIException.class})
    private String commitInRemoteRepository(String fileName, String content) {
        try (Git git = Git.open(remoteRepository.toFile())) {
            Files.writeString(remoteRepository.resolve(fileName), content);
            git.add().addFilepattern(fileName).call();
            RevCommit commit = git.commit().setMessage("Add " + fileName).call();
            return commit.getName();
        }
    }

    @SneakyThrows({IOException.class})
    private String remoteHeadHash() {
        try (Git git = Git.open(remoteRepository.toFile())) {
            return git.getRepository().resolve("HEAD").getName();
        }
    }
}
