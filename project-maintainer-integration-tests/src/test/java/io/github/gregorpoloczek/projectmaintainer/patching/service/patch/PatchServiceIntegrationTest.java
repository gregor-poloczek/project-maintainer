package io.github.gregorpoloczek.projectmaintainer.patching.service.patch;

import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.PullRequest;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.Workspace;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.WorkspaceService;
import io.github.gregorpoloczek.projectmaintainer.integrationtests.TestApplication;
import io.github.gregorpoloczek.projectmaintainer.integrationtests.support.IntegrationTestCleanupExtension;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.parameters.exceptions.PatchParameterArgumentMissingException;
import io.github.gregorpoloczek.projectmaintainer.integrationtests.support.IntegrationTestFileSystemProjectDiscovery;
import io.github.gregorpoloczek.projectmaintainer.integrationtests.support.TestPatchArguments;
import io.github.gregorpoloczek.projectmaintainer.integrationtests.support.patches.MultipurposeTestPatch;
import io.github.gregorpoloczek.projectmaintainer.integrationtests.support.patches.NoOpTestPatch;
import io.github.gregorpoloczek.projectmaintainer.integrationtests.support.IntegrationTestFileSystemProjectConnection;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchExecutionResult;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchStopResult;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.parameters.WellKnownPatchParameters;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchService;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.UnifiedDiffFile;
import io.github.gregorpoloczek.projectmaintainer.patching.spi.patch.common.PatchMetaData;
import io.github.gregorpoloczek.projectmaintainer.scm.service.git.GitService;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopyService;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static io.github.gregorpoloczek.projectmaintainer.integrationtests.support.OperationProgressAssertions.requireDone;
import static io.github.gregorpoloczek.projectmaintainer.integrationtests.support.PatchOperationAssertions.requireDetail;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
@ExtendWith(IntegrationTestCleanupExtension.class)
public class PatchServiceIntegrationTest {

    public static final int DIFF_CONTEXT_SIZE = 2;
    private static final String DEFAULT_BRANCH = "master";
    private static final String PATCH_BRANCH = "project-maintainer/" + MultipurposeTestPatch.ID;

    public static class RepositoryToc {
        public static final String NEWFILE_TXT = "newfile.txt";
        public static final String ONE_TXT = "one.txt";
        public static final String TWO_TXT = "two.txt";
        public static final String THREE_TXT = "three.txt";
    }

    public static class SubModuleRepositoryToc {
        public static final String FRUIT_TXT = "fruit.txt";
    }

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

    private Path remoteRepository1;
    private Path remoteSubModuleRepository1;


    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("project-maintainer.workspaces-directory", () -> workspacesDirectory.toString());
    }

    @BeforeEach
    void setUp() {
        this.remoteSubModuleRepository1 = this.createSubModuleRepository();
        this.remoteRepository1 = this.createMainRepository();
    }

    @AfterEach
    void tearDown() throws IOException {
        for (Path path : List.of(this.remoteRepository1, this.remoteSubModuleRepository1)) {
            if (path != null) {
                FileUtils.deleteDirectory(path.toFile());
            }
        }
    }

    @Test
    void testPreviewWithEmptyPatch() {
        Project project = createWorkspaceWithSingleRepository();

        // create a preview
        ProjectOperationProgress<PatchExecutionResult> previewProgress =
                requireDone(patchService.previewPatch(project, NoOpTestPatch.ID, List.of(), DIFF_CONTEXT_SIZE));

        assertThat(requireDetail(previewProgress, PatchExecutionResult.NoopResultDetail.class)).satisfies(detail -> {
            assertThat(detail.getName()).isEqualTo("No-Op");
            assertThat(detail.getDescription()).isEqualTo("Patch did not change any files.");
        });
    }

    @Test
    void testApplyWithMissingArgument() {
        Project project = createWorkspaceWithSingleRepository();

        // the patch declares parameters, but no arguments are passed for them
        assertThatThrownBy(() -> patchService.applyPatch(project, MultipurposeTestPatch.ID, List.of(), DIFF_CONTEXT_SIZE))
                .isInstanceOfSatisfying(PatchParameterArgumentMissingException.class, e -> {
                    assertThat(e.getPatchId()).isEqualTo(MultipurposeTestPatch.ID);
                    assertThat(e.getParameterId()).isEqualTo(MultipurposeTestPatch.Parameters.ADD_FILENAME);
                });
    }

    @Test
    void testPreviewWithFileManipulationPatch() {
        Project project = createWorkspaceWithSingleRepository();

        ProjectOperationProgress<PatchExecutionResult> previewProgress =
                requireDone(patchService.previewPatch(project, MultipurposeTestPatch.ID,
                        arguments(MultipurposeTestPatch.ID)
                                .argument(MultipurposeTestPatch.Parameters.ADD_FILENAME, RepositoryToc.NEWFILE_TXT)
                                .argument(MultipurposeTestPatch.Parameters.EDIT_FILENAME, RepositoryToc.TWO_TXT)
                                .argument(MultipurposeTestPatch.Parameters.DELETE_FILENAME, RepositoryToc.THREE_TXT)
                                .emptyRemaining(),
                        DIFF_CONTEXT_SIZE)
                );

        PatchExecutionResult.PreviewGeneratedResultDetail detail = requireDetail(previewProgress, PatchExecutionResult.PreviewGeneratedResultDetail.class);
        assertThat(detail.getName()).isEqualTo("Preview Generated");
        assertThat(detail.getDescription()).isEqualTo("Preview of all projected changes generated.");

        List<UnifiedDiffFile> diffs = detail.getUnifiedDiff().getFiles();

        assertThat(diffs).hasSize(3);

        // added file
        assertThat(diffs.get(0).getLines())
                .containsExactly("diff --git a/newfile.txt b/newfile.txt",
                        "new file mode 100644",
                        "index 0000000..4fe0a66",
                        "--- /dev/null",
                        "+++ b/newfile.txt",
                        "@@ -0,0 +1 @@",
                        "+My-Content",
                        "\\ No newline at end of file");
        // edited file
        assertThat(diffs.get(2).getLines())
                .containsExactly("diff --git a/two.txt b/two.txt",
                        "index 2ef7ae0..4fe0a66 100644",
                        "--- a/two.txt",
                        "+++ b/two.txt",
                        "@@ -1 +1 @@",
                        "-# Two",
                        "+My-Content",
                        "\\ No newline at end of file");
        // deleted file
        assertThat(diffs.get(1).getLines())
                .containsExactly("diff --git a/three.txt b/three.txt",
                        "deleted file mode 100644",
                        "index fac55f7..0000000",
                        "--- a/three.txt",
                        "+++ /dev/null",
                        "@@ -1 +0,0 @@",
                        "-# Three");

        // TODO [Patching] test operations
    }

    @Test
    void testApplyWithFileManipulationPatch() {
        Project project = createWorkspaceWithSingleRepository();

        ProjectOperationProgress<PatchExecutionResult> applyProgress =
                requireDone(patchService.applyPatch(project, MultipurposeTestPatch.ID,
                        arguments(MultipurposeTestPatch.ID)
                                .argument(MultipurposeTestPatch.Parameters.ADD_FILENAME, RepositoryToc.NEWFILE_TXT)
                                .argument(MultipurposeTestPatch.Parameters.EDIT_FILENAME, RepositoryToc.TWO_TXT)
                                .argument(MultipurposeTestPatch.Parameters.DELETE_FILENAME, RepositoryToc.THREE_TXT)
                                .emptyRemaining(),
                        DIFF_CONTEXT_SIZE));

        PatchExecutionResult.AppliedResultDetail detail = requireDetail(applyProgress, PatchExecutionResult.AppliedResultDetail.class);
        assertThat(detail.getName()).isEqualTo("Patch applied");
        assertThat(detail.getDescription()).isEqualTo("All projected changes were applied in a remote branch, and a pull request was created.");

        assertThat(detail.getRemoteBranch().getName()).isEqualTo(PATCH_BRANCH);
        assertThat(detail.getPullRequest().getSourceBranchName()).isEqualTo(PATCH_BRANCH);
        assertThat(detail.getPullRequest().getTargetBranchName()).isEqualTo(DEFAULT_BRANCH);
        // without override, the pull request title is the description of the patch
        assertThat(detail.getPullRequest().getTitle()).isEqualTo(MultipurposeTestPatch.ID);
        // without override, the commit message is derived from id and description of the patch
        assertThat(detail.getCommitMessage()).isEqualTo("Applying patch \"MultipurposeTestPatch\": MultipurposeTestPatch");

        // the pull request reported in the result is the one that has been created
        List<PullRequest> pullRequests = Objects.requireNonNull(integrationTestFileSystemProjectDiscovery.getOpenPullRequests(project).block());
        assertThat(pullRequests).singleElement()
                .extracting(PullRequest::getId).isEqualTo(detail.getPullRequest().getId());

        // the patch branch in the remote repository contains exactly one commit on top of the default branch ...
        assertThat(remoteCommitsBetween(DEFAULT_BRANCH, PATCH_BRANCH)).singleElement()
                .extracting(RevCommit::getFullMessage).isEqualTo(detail.getCommitMessage());

        // ... which contains exactly the changes made by the patch, and nothing else (e.g. the submodule is untouched)
        assertThat(remoteDiff(DEFAULT_BRANCH, PATCH_BRANCH))
                .extracting(DiffEntry::getChangeType, e -> e.getChangeType() == DiffEntry.ChangeType.DELETE ? e.getOldPath() : e.getNewPath())
                .containsExactlyInAnyOrder(
                        tuple(DiffEntry.ChangeType.ADD, RepositoryToc.NEWFILE_TXT),
                        tuple(DiffEntry.ChangeType.MODIFY, RepositoryToc.TWO_TXT),
                        tuple(DiffEntry.ChangeType.DELETE, RepositoryToc.THREE_TXT));
        assertThat(readRemoteFile(PATCH_BRANCH, RepositoryToc.NEWFILE_TXT)).contains("My-Content");
        assertThat(readRemoteFile(PATCH_BRANCH, RepositoryToc.TWO_TXT)).contains("My-Content");
        assertThat(readRemoteFile(PATCH_BRANCH, RepositoryToc.THREE_TXT)).isEmpty();

        // the default branch has not been changed, changes only get there by merging the pull request
        assertThat(readRemoteFile(DEFAULT_BRANCH, RepositoryToc.NEWFILE_TXT)).isEmpty();
        assertThat(readRemoteFile(DEFAULT_BRANCH, RepositoryToc.TWO_TXT)).contains("# Two\n");
        assertThat(readRemoteFile(DEFAULT_BRANCH, RepositoryToc.THREE_TXT)).contains("# Three\n");
    }

    @Test
    void testApplyWithExistingRemoteBranch() {
        // the patch branch already exists in the remote repository, but there is no pull request for it
        // (it has to exist before attaching the project, so that the working copy knows about it)
        createRemoteBranch(PATCH_BRANCH);
        Project project = createWorkspaceWithSingleRepository();

        ProjectOperationProgress<PatchExecutionResult> applyProgress =
                requireDone(patchService.applyPatch(project, MultipurposeTestPatch.ID, addFileArguments(),
                        DIFF_CONTEXT_SIZE));

        // the patch is blocked by the existing branch
        var detail = requireDetail(applyProgress, PatchExecutionResult.RemoteBranchExistsResultDetail.class);
        assertThat(detail.getRemoteBranch().getName()).isEqualTo(PATCH_BRANCH);

        // neither the branch has been changed, nor a pull request has been created
        assertThat(remoteCommitsBetween(DEFAULT_BRANCH, PATCH_BRANCH)).isEmpty();
        assertThat(integrationTestFileSystemProjectDiscovery.getOpenPullRequests(project).block()).isEmpty();
    }

    @Test
    void testApplyWithExistingPullRequest() {
        Project project = createWorkspaceWithSingleRepository();

        // a first application creates the remote branch and the pull request
        requireDone(patchService.applyPatch(project, MultipurposeTestPatch.ID, addFileArguments(), DIFF_CONTEXT_SIZE));
        PullRequest pullRequest = Objects.requireNonNull(
                integrationTestFileSystemProjectDiscovery.getOpenPullRequests(project).block()).getFirst();

        // applying the patch again
        ProjectOperationProgress<PatchExecutionResult> applyProgress =
                requireDone(patchService.applyPatch(project, MultipurposeTestPatch.ID, addFileArguments(),
                        DIFF_CONTEXT_SIZE));

        // the patch is blocked by the open pull request
        var detail = requireDetail(applyProgress, PatchExecutionResult.PullRequestStillOpenResultDetail.class);
        assertThat(detail.getPullRequest().getId()).isEqualTo(pullRequest.getId());
        assertThat(detail.getRemoteBranch().getName()).isEqualTo(PATCH_BRANCH);

        // the existing branch and pull request remain as they were, nothing has been applied a second time
        assertThat(remoteCommitsBetween(DEFAULT_BRANCH, PATCH_BRANCH)).hasSize(1);
        assertThat(integrationTestFileSystemProjectDiscovery.getOpenPullRequests(project).block())
                .singleElement().extracting(PullRequest::getId).isEqualTo(pullRequest.getId());
    }

    @Test
    void testApplyWithBranchOverride() {
        Project project = createWorkspaceWithSingleRepository();
        String customBranch = "custom/my-branch";

        ProjectOperationProgress<PatchExecutionResult> applyProgress =
                requireDone(patchService.applyPatch(project, MultipurposeTestPatch.ID,
                        addFileArguments().argument(WellKnownPatchParameters.BRANCH, customBranch),
                        DIFF_CONTEXT_SIZE));

        // the patch is applied in the given branch instead of the default patch branch
        var detail = requireDetail(applyProgress, PatchExecutionResult.AppliedResultDetail.class);
        assertThat(detail.getRemoteBranch().getName()).isEqualTo(customBranch);
        assertThat(detail.getPullRequest().getSourceBranchName()).isEqualTo(customBranch);

        assertThat(remoteBranchExists(customBranch)).isTrue();
        assertThat(remoteBranchExists(PATCH_BRANCH)).isFalse();
    }

    @Test
    void testApplyWithBlankBranchOverride() {
        Project project = createWorkspaceWithSingleRepository();

        ProjectOperationProgress<PatchExecutionResult> applyProgress =
                requireDone(patchService.applyPatch(project, MultipurposeTestPatch.ID,
                        addFileArguments().argument(WellKnownPatchParameters.BRANCH, " "),
                        DIFF_CONTEXT_SIZE));

        // a blank branch is ignored, the default patch branch ("project-maintainer/<patch id>") is used instead
        var detail = requireDetail(applyProgress, PatchExecutionResult.AppliedResultDetail.class);
        assertThat(detail.getRemoteBranch().getName()).isEqualTo(PATCH_BRANCH);
        assertThat(remoteBranchExists(PATCH_BRANCH)).isTrue();
    }

    @Test
    void testApplyWithPullRequestTitleOverride() {
        Project project = createWorkspaceWithSingleRepository();
        String customTitle = "My custom title";

        // the patch itself overrides the pull request title
        ProjectOperationProgress<PatchExecutionResult> applyProgress =
                requireDone(patchService.applyPatch(project, MultipurposeTestPatch.ID,
                        arguments(MultipurposeTestPatch.ID)
                                .argument(MultipurposeTestPatch.Parameters.ADD_FILENAME, RepositoryToc.NEWFILE_TXT)
                                .argument(MultipurposeTestPatch.Parameters.PULL_REQUEST_TITLE, customTitle)
                                .emptyRemaining(),
                        DIFF_CONTEXT_SIZE));

        var detail = requireDetail(applyProgress, PatchExecutionResult.AppliedResultDetail.class);
        assertThat(detail.getPullRequest().getTitle()).isEqualTo(customTitle);
        assertThat(integrationTestFileSystemProjectDiscovery.getOpenPullRequests(project).block())
                .singleElement().extracting(PullRequest::getTitle).isEqualTo(customTitle);
    }

    @Test
    void testApplyWithCommitMessageOverride() {
        Project project = createWorkspaceWithSingleRepository();
        String customCommitMessage = "My custom commit message";

        // the patch itself overrides the commit message
        ProjectOperationProgress<PatchExecutionResult> applyProgress =
                requireDone(patchService.applyPatch(project, MultipurposeTestPatch.ID,
                        arguments(MultipurposeTestPatch.ID)
                                .argument(MultipurposeTestPatch.Parameters.ADD_FILENAME, RepositoryToc.NEWFILE_TXT)
                                .argument(MultipurposeTestPatch.Parameters.COMMIT_MESSAGE, customCommitMessage)
                                .emptyRemaining(),
                        DIFF_CONTEXT_SIZE));

        var detail = requireDetail(applyProgress, PatchExecutionResult.AppliedResultDetail.class);
        assertThat(detail.getCommitMessage()).isEqualTo(customCommitMessage);
        // the commit in the remote branch carries the overridden message as well
        assertThat(remoteCommitsBetween(DEFAULT_BRANCH, PATCH_BRANCH)).singleElement()
                .extracting(RevCommit::getFullMessage).isEqualTo(customCommitMessage);
    }

    @Test
    void testStopWithoutPullRequestAndWithoutRemoteBranch() {
        Project project = createWorkspaceWithSingleRepository();

        // nothing was applied before, hence there is nothing to stop
        ProjectOperationProgress<PatchStopResult> stopProgress =
                requireDone(patchService.stopPatch(project, MultipurposeTestPatch.ID,
                        emptyMultipurposeArguments()));

        requireDetail(stopProgress, PatchStopResult.NoopResultDetail.class);
    }

    @Test
    void testStopWithoutPullRequestButWithRemoteBranch() {
        // the patch branch exists in the remote repository (e.g. the pull request was already closed manually),
        // it has to exist before attaching the project, so that the working copy knows about it
        createRemoteBranch(PATCH_BRANCH);
        Project project = createWorkspaceWithSingleRepository();

        ProjectOperationProgress<PatchStopResult> stopProgress =
                requireDone(patchService.stopPatch(project, MultipurposeTestPatch.ID,
                        emptyMultipurposeArguments()));

        var detail = requireDetail(stopProgress, PatchStopResult.DoneResultDetail.class);
        assertThat(detail.getRemoteBranch().getName()).isEqualTo(PATCH_BRANCH);
        assertThat(detail.getPullRequest()).isEmpty();

        // the remote branch has been deleted
        assertThat(remoteBranchExists(PATCH_BRANCH)).isFalse();
    }

    @Test
    void testStopWithPullRequest() {
        Project project = createWorkspaceWithSingleRepository();

        // applying the patch creates both the remote branch and the pull request
        requireDone(patchService.applyPatch(project, MultipurposeTestPatch.ID,
                addFileArguments(),
                DIFF_CONTEXT_SIZE));
        assertThat(remoteBranchExists(PATCH_BRANCH)).isTrue();
        PullRequest pullRequest = Objects.requireNonNull(
                integrationTestFileSystemProjectDiscovery.getOpenPullRequests(project).block()).getFirst();

        ProjectOperationProgress<PatchStopResult> stopProgress =
                requireDone(patchService.stopPatch(project, MultipurposeTestPatch.ID,
                        emptyMultipurposeArguments()));

        var detail = requireDetail(stopProgress, PatchStopResult.DoneResultDetail.class);
        assertThat(detail.getRemoteBranch().getName()).isEqualTo(PATCH_BRANCH);
        assertThat(detail.getPullRequest()).get().extracting(PullRequest::getId).isEqualTo(pullRequest.getId());

        // the pull request has been closed and the remote branch has been deleted
        assertThat(integrationTestFileSystemProjectDiscovery.getOpenPullRequests(project).block()).isEmpty();
        assertThat(remoteBranchExists(PATCH_BRANCH)).isFalse();
    }

    @Test
    void testStopWithBranchOverride() {
        Project project = createWorkspaceWithSingleRepository();
        String customBranch = "custom/my-branch";

        // the patch has been applied in a custom branch
        requireDone(patchService.applyPatch(project, MultipurposeTestPatch.ID,
                addFileArguments().argument(WellKnownPatchParameters.BRANCH, customBranch), DIFF_CONTEXT_SIZE));

        // stopping with the same branch finds the pull request and the branch
        ProjectOperationProgress<PatchStopResult> stopProgress =
                requireDone(patchService.stopPatch(project, MultipurposeTestPatch.ID,
                        emptyMultipurposeArguments().argument(WellKnownPatchParameters.BRANCH, customBranch)));

        var detail = requireDetail(stopProgress, PatchStopResult.DoneResultDetail.class);
        assertThat(detail.getRemoteBranch().getName()).isEqualTo(customBranch);
        assertThat(detail.getPullRequest()).isPresent();
        assertThat(integrationTestFileSystemProjectDiscovery.getOpenPullRequests(project).block()).isEmpty();
        assertThat(remoteBranchExists(customBranch)).isFalse();
    }

    @Test
    void testStopWithDifferentBranch() {
        Project project = createWorkspaceWithSingleRepository();
        String customBranch = "custom/my-branch";

        // the patch has been applied in a custom branch
        requireDone(patchService.applyPatch(project, MultipurposeTestPatch.ID,
                addFileArguments().argument(WellKnownPatchParameters.BRANCH, customBranch), DIFF_CONTEXT_SIZE));

        // stopping without the custom branch looks for the default patch branch, hence nothing is found
        ProjectOperationProgress<PatchStopResult> stopProgress =
                requireDone(patchService.stopPatch(project, MultipurposeTestPatch.ID, emptyMultipurposeArguments()));

        requireDetail(stopProgress, PatchStopResult.NoopResultDetail.class);
        // the pull request and the custom branch remain untouched
        assertThat(integrationTestFileSystemProjectDiscovery.getOpenPullRequests(project).block()).hasSize(1);
        assertThat(remoteBranchExists(customBranch)).isTrue();
    }

    @Test
    void testApplyWithEmptyPatch() {
        Project project = createWorkspaceWithSingleRepository();

        // apply patch (which does nothing)
        ProjectOperationProgress<PatchExecutionResult> applyProgress =
                requireDone(patchService.applyPatch(project, NoOpTestPatch.ID, List.of(), DIFF_CONTEXT_SIZE));

        var detail = requireDetail(applyProgress, PatchExecutionResult.NoopResultDetail.class);
        assertThat(detail.getName()).isEqualTo("No-Op");
        assertThat(detail.getDescription()).isEqualTo("Patch did not change any files.");
    }

    @Test
    void testGetAvailablePatches() {
        assertThat(patchService.getAvailablePatches())
                .extracting(PatchMetaData::getId)
                .contains(MultipurposeTestPatch.ID, NoOpTestPatch.ID);
    }

    private TestPatchArguments arguments(String patchId) {
        return patchTestArgumentsProvider.getObject(patchId);
    }

    private TestPatchArguments addFileArguments() {
        // a single change is sufficient for the patch to create a remote branch and a pull request
        return arguments(MultipurposeTestPatch.ID)
                .argument(MultipurposeTestPatch.Parameters.ADD_FILENAME, RepositoryToc.NEWFILE_TXT)
                .emptyRemaining();
    }

    private TestPatchArguments emptyMultipurposeArguments() {
        // all parameters are optional, but an argument (without value) has to be passed for each of them
        return arguments(MultipurposeTestPatch.ID)
                .emptyRemaining();
    }

    @SneakyThrows({IOException.class, GitAPIException.class})
    private void createRemoteBranch(String branch) {
        try (Git git = Git.open(remoteRepository1.toFile())) {
            git.branchCreate().setName(branch).call();
        }
    }

    @SneakyThrows({IOException.class})
    private boolean remoteBranchExists(String branch) {
        try (Git git = Git.open(remoteRepository1.toFile())) {
            return git.getRepository().findRef("refs/heads/" + branch) != null;
        }
    }

    /**
     * Reads a file from the given branch of the remote repository.
     */
    @SneakyThrows({IOException.class})
    private Optional<String> readRemoteFile(String branch, String path) {
        try (Git git = Git.open(remoteRepository1.toFile());
             RevWalk revWalk = new RevWalk(git.getRepository())) {
            Repository repository = git.getRepository();
            RevCommit commit = revWalk.parseCommit(repository.resolve("refs/heads/" + branch));
            try (TreeWalk treeWalk = TreeWalk.forPath(repository, path, commit.getTree())) {
                if (treeWalk == null) {
                    return Optional.empty();
                }
                return Optional.of(new String(repository.open(treeWalk.getObjectId(0)).getBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * Returns the commits of the remote repository that are reachable from {@code branch}, but not from {@code base}.
     */
    @SneakyThrows({IOException.class, GitAPIException.class})
    private List<RevCommit> remoteCommitsBetween(String base, String branch) {
        try (Git git = Git.open(remoteRepository1.toFile())) {
            Repository repository = git.getRepository();
            return StreamSupport.stream(git.log()
                            .addRange(repository.resolve("refs/heads/" + base), repository.resolve("refs/heads/" + branch))
                            .call().spliterator(), false)
                    .toList();
        }
    }

    /**
     * Returns the file changes between two branches of the remote repository.
     */
    @SneakyThrows({IOException.class, GitAPIException.class})
    private List<DiffEntry> remoteDiff(String base, String branch) {
        try (Git git = Git.open(remoteRepository1.toFile());
             ObjectReader reader = git.getRepository().newObjectReader()) {
            Repository repository = git.getRepository();
            return git.diff()
                    .setOldTree(new CanonicalTreeParser(null, reader, repository.resolve("refs/heads/" + base + "^{tree}")))
                    .setNewTree(new CanonicalTreeParser(null, reader, repository.resolve("refs/heads/" + branch + "^{tree}")))
                    .call();
        }
    }

    private @NonNull Project createWorkspaceWithSingleRepository() {
        // create workspace and discover all projects
        Workspace workspace = workspaceService.createWorkspace(PatchServiceIntegrationTest.class.getSimpleName());

        workspace = workspaceService.updateConnections(workspace,
                List.of(IntegrationTestFileSystemProjectConnection.builder()
                        .remoteRepository(remoteRepository1)
                        .build()));

        requireDone(workspaceService.discoverProjects(workspace));

        // determine project
        List<Project> projects = projectService.findAllByWorkspaceId(workspace.getId());
        assertThat(projects).hasSize(1);
        Project project = projects.getFirst();

        // attach project
        requireDone(workingCopyService.attachProject(project));
        return project;
    }

    @SneakyThrows({IOException.class, GitAPIException.class})
    private @NonNull Path createMainRepository() {
        Path result = remoteRepositoriesDirectory.resolve("repository-1");
        Files.createDirectory(result);

        try (Git git = Git.init()
                .setDirectory(result.toFile())
                .call()) {
            Path readme = result.resolve("README.md");
            createFile(readme, "# Some Project");

            createFile(result.resolve(RepositoryToc.ONE_TXT), "# One\n");
            createFile(result.resolve(RepositoryToc.TWO_TXT), "# Two\n");
            createFile(result.resolve(RepositoryToc.THREE_TXT), "# Three\n");

            git.add().addFilepattern(".").call();
            git.commit().setMessage("Initial commit").call();

            git.submoduleAdd().setURI(remoteSubModuleRepository1.toUri().toString()).setPath("submodule").call();
            git.add().addFilepattern(".gitmodules").call();
            git.add().addFilepattern("submodule").call();
            git.commit().setMessage("Add submodule").call();
            return result;
        }
    }

    @SneakyThrows({IOException.class, GitAPIException.class})
    private @NonNull Path createSubModuleRepository() {
        Path result = remoteRepositoriesDirectory.resolve("submodule-repository-1");
        Files.createDirectory(result);

        try (Git git = Git.init()
                .setDirectory(result.toFile())
                .call()) {
            Path readme = result.resolve("README.md");
            createFile(readme, "# Sub-Module Project");

            createFile(result.resolve(SubModuleRepositoryToc.FRUIT_TXT), "# Apples\n");

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
}
