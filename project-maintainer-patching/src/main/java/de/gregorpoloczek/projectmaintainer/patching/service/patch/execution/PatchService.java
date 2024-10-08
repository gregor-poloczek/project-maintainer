package de.gregorpoloczek.projectmaintainer.patching.service.patch.execution;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress.State;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress.ProjectOperationProgressBuilder;
import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscovery.PullRequestCreation;
import de.gregorpoloczek.projectmaintainer.core.domain.discovery.service.PullRequest;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.Patch;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.PatchMetaData;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.ProjectFileCreation;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.ProjectFileDeletion;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.ProjectFileOperation;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.ProjectFileOperationType;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.definition.ProjectFileUpdate;
import de.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchExecutionResult.PreviewGeneratedResultDetail;
import de.gregorpoloczek.projectmaintainer.scm.service.git.BranchState;
import de.gregorpoloczek.projectmaintainer.scm.service.git.GitService;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import de.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopyService;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.transport.RefSpec;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PatchService {

    WorkingCopyService workingCopyService;
    List<Patch> patches;
    ProjectService projectService;
    GitService gitService;
    private final ConversionService conversionService;

    public List<PatchMetaData> getAvailablePatches() {
        return this.patches.stream().map(Patch::getMetaData).toList();
    }

    public Flux<ProjectOperationProgress<PatchExecutionResult>> applyPatch(
            ProjectRelatable projectRelatable, String id) {
        return usePatch(projectRelatable, id, false);
    }

    public Flux<ProjectOperationProgress<PatchExecutionResult>> previewPatch(ProjectRelatable projectRelatable,
            String id) {
        return usePatch(projectRelatable, id, true);
    }


    public Flux<ProjectOperationProgress<PatchStopResult>> stopPatch(ProjectRelatable projectRelatable,
            String id) {
        Patch patch = requirePatch(id);

        return Flux.create(sink -> {
            FQPN fqpn = projectRelatable.getFQPN();
            ProjectOperationProgressBuilder<PatchStopResult> progress =
                    ProjectOperationProgress.<PatchStopResult>builder().fqpn(fqpn);

            ProgressSink<PatchStopResult> progressSink = new ProgressSink<>(sink, progress);
            sink.next(progress.state(State.SCHEDULED).build());

            progressSink.total(2);
            progress.state(State.RUNNING);

            WorkingCopy workingCopy = workingCopyService.require(projectRelatable);
            String defaultBranch = this.gitService.getBranchState(workingCopy).getDefaultBranch();
            PatchStopContext stopContext = PatchStopContext.builder()
                    .patch(patch)
                    .workingCopy(workingCopy)
                    .patchBranch(getPatchBranch(patch))
                    .baseBranch(defaultBranch)
                    .progressSink(progressSink)
                    .defaultBranch(defaultBranch)
                    .build();

            declinePullRequest(stopContext)
                    .flatMap(pullRequest -> deleteRemoteBranch(stopContext, pullRequest))
                    .map(detail -> PatchStopResult.builder().detail(detail).build())
                    .map(result -> progress.state(State.DONE)
                            .progressCurrent(1)
                            .progressTotal(1)
                            .result(result)
                            .message(null)
                            .build())
                    .subscribe(r -> {
                        sink.next(r);
                        sink.complete();
                    });

        });
    }

    private Mono<? extends PatchOperationResultDetail> deleteRemoteBranch(PatchStopContext stopContext,
            Optional<PullRequest> pullRequest) {
        return Mono.fromSupplier(() -> {
            SortedSet<String> remoteBranches = this.gitService.getBranchState(stopContext.getWorkingCopy())
                    .getRemoteBranches();
            if (remoteBranches.contains(stopContext.getPatchBranch())) {
                log.info("Deleting branch \"{}\" from \"{}\".", stopContext.getPatchBranch(), stopContext.getFQPN());
                RemoteBranch branch =
                        RemoteBranch.builder()
                                .name(stopContext.getPatchBranch())
                                .href(getPatchRemoteBranchHref(stopContext))
                                .build();
                gitService.execute(stopContext.getWorkingCopy(), git -> {
                    // TODO where is the delete operation?
                    // delete origin branch
                    git.push()
                            .setRemote("origin")
                            .setRefSpecs(new RefSpec(":refs/heads/" + stopContext.getPatchBranch()))
                            .setCredentialsProvider(stopContext.getWorkingCopy().getCredentialsProvider())
                            .call();

                    // delete local branch by pruning
                    git.fetch()
                            .setRemoveDeletedRefs(true)
                            .setCredentialsProvider(stopContext.getWorkingCopy().getCredentialsProvider())
                            .call();
                });

                return PatchStopResult.DoneResultDetail.builder()
                        .remoteBranch(branch)
                        .pullRequest(pullRequest.orElse(null))
                        .build();
            } else {
                return PatchStopResult.NoopResultDetail.builder().build();
            }
        }).doOnSubscribe(_ -> stopContext.publish("Deleting patch branch"));
    }

    private Mono<Optional<PullRequest>> declinePullRequest(PatchStopContext stopContext) {
        return getRelevantPullRequest(stopContext)
                .flatMap(pullRequest -> {
                    if (pullRequest.isEmpty()) {
                        log.info("No open pull request \"{}\" found \"{}\".",
                                getPullRequestTitle(stopContext.getPatch()), stopContext.getFQPN());
                        return Mono.just(pullRequest);
                    } else {
                        log.info("Closing pull request \"{}\" from \"{}\".",
                                getPullRequestTitle(stopContext.getPatch()), stopContext.getFQPN());
                        // TODO add pull request to result
                        return
                                this.gitService.closePullRequest(stopContext, pullRequest.get())
                                        .then(Mono.just(pullRequest));
                    }
                })
                .doOnSubscribe(_ -> stopContext.publish("Close pull request"));
    }

    private Flux<ProjectOperationProgress<PatchExecutionResult>> usePatch(ProjectRelatable projectRelatable,
            String id, boolean previewOnly) {
        Patch patch = this.requirePatch(id);
        FQPN fqpn = projectRelatable.getFQPN();
        WorkingCopy workingCopy = this.workingCopyService.require(projectRelatable);

        return Flux.create(sink -> {
            ProjectOperationProgressBuilder<PatchExecutionResult> progress =
                    ProjectOperationProgress.<PatchExecutionResult>builder().fqpn(fqpn);

            ProgressSink<PatchExecutionResult> progressSink = new ProgressSink<>(sink, progress);

            sink.next(progress.state(State.SCHEDULED).build());
            progressSink.total(previewOnly ? 4 : 6);
            progress.state(State.RUNNING);

            String defaultBranch = this.gitService.getBranchState(workingCopy).getDefaultBranch();
            PatchExecutionContext executionContext = PatchExecutionContext.builder()
                    .workingCopy(workingCopy)
                    .progressSink(progressSink)
                    .patch(patch)
                    .defaultBranch(defaultBranch)
                    .baseBranch(defaultBranch)
                    .patchBranch(this.getPatchBranch(patch))
                    .build();

            // TODO lock working copy

            log.info("A");
            // TODO umbauen auf ohne "switchIfEmpty"
            Mono.empty()
                    .then(this.resetWorkspace(executionContext))
                    .then(this.checkForExistingPullRequest(executionContext))
                    .switchIfEmpty(this.checkForExistingRemoteBranch(executionContext))
                    .switchIfEmpty(this.makeSourceCodeChanges(executionContext))
                    .flatMap(detail -> {
                        if (!(detail instanceof PreviewGeneratedResultDetail)) {
                            return Mono.just(detail);
                        } else if (!previewOnly) {
                            return this.createBranchWithChanges(executionContext, (PreviewGeneratedResultDetail) detail)
                                    .flatMap(remoteBranch -> this.createPullRequest(executionContext, remoteBranch));
                        } else {
                            log.info("Patch {} in {} is a dry run, not applying changes", id, fqpn);
                            return Mono.just(detail);
                        }
                    })
                    .subscribe(detail -> {
                                sink.next(
                                        progress.state(State.DONE)
                                                .progressCurrent(1)
                                                .progressTotal(1)
                                                .result(PatchExecutionResult.builder().detail(detail).build())
                                                .build());
                                sink.complete();
                            }, sink::error
                    );
        });
    }


    private Mono<? extends PatchOperationResultDetail> createPullRequest(PatchExecutionContext executionContext,
            RemoteBranch remoteBranch) {
        PullRequestCreation pullRequestCreation = PullRequestCreation.builder()
                .title(getPullRequestTitle(executionContext.getPatch()))
                .sourceBranchName(executionContext.getPatchBranch())
                .targetBranchName(executionContext.getDefaultBranch())
                .build();
        return gitService
                .createPullRequest(executionContext.getWorkingCopy(), pullRequestCreation)
                .map(pR ->
                        PatchExecutionResult.AppliedResultDetail.builder()
                                .remoteBranch(remoteBranch)
                                .pullRequest(pR)
                                .build())
                .doOnSubscribe(_ -> executionContext.publish("Creating pull request"));
    }

    private String getPullRequestTitle(Patch patch) {
        // TODO better title
        return patch.getMetaData().getDescription();
    }

    private Mono<RemoteBranch> createBranchWithChanges(
            PatchExecutionContext executionContext,
            PreviewGeneratedResultDetail detail
    ) {
        return Mono.fromSupplier(() -> {

            gitService.execute(executionContext.getWorkingCopy(), git -> {
                BranchState branchState = this.gitService.getBranchState(executionContext.getWorkingCopy(), git);

                git.checkout().setName(branchState.getDefaultBranch()).call();

                try {
                    // delete previously created local branch (in case it was retained)
                    if (branchState.getLocalBranches().contains(executionContext.getPatchBranch())) {
                        log.warn("Detected obsolete local branch {} in {}, deleting it.",
                                executionContext.getPatchBranch(),
                                executionContext.getFQPN());
                        // delete local branch
                        git.branchDelete().setForce(true).setBranchNames(executionContext.getPatchBranch()).call();
                    }

                    // create local branch
                    git.checkout().setCreateBranch(true).setName(executionContext.getPatchBranch()).call();

                    // push branch to remote
                    git.push()
                            .setRemote("origin")
                            .add(executionContext.getPatchBranch())
                            .setCredentialsProvider(executionContext.getWorkingCopy().getCredentialsProvider())
                            .call();

                    // apply changes in file system
                    this.applyOperationsToFileSystem(detail.getOperations());

                    // stage changes
                    for (ProjectFileOperation o : detail.getOperations()) {
                        String pattern = o.getLocation().getRelativePath().toString();
                        if (o instanceof ProjectFileDeletion) {
                            git.rm().addFilepattern(pattern).call();
                        } else {
                            git.add().addFilepattern(pattern).call();
                        }
                    }

                    // commit changes
                    git.commit().setMessage(getCommitMessage(executionContext.getPatch())).call();

                    // push changes
                    git.push()
                            .setCredentialsProvider(executionContext.getWorkingCopy().getCredentialsProvider())
                            .call();

                    git.checkout().setName(branchState.getDefaultBranch()).call();
                    git.branchDelete().setBranchNames(executionContext.getPatchBranch()).setForce(true).call();
                } finally {
                    git.reset().setMode(ResetType.HARD).call();
                    git.checkout().setName(branchState.getDefaultBranch()).call();
                }
            });
            return RemoteBranch.builder()
                    .name(executionContext.getPatchBranch())
                    .href(getPatchRemoteBranchHref(executionContext))
                    .build();
        }).doOnSubscribe(_ -> executionContext.publish("Applying changes"));
    }

    private String getCommitMessage(Patch patch) {
        PatchMetaData metaData = patch.getMetaData();
        String prefix = metaData.getCommitPrefix().map(p -> p + " ").orElse("");
        return prefix + metaData.getDescription();
    }

    private void applyOperationsToFileSystem(List<ProjectFileOperation> operations) {
        for (ProjectFileOperation operation : operations) {
            switch (operation) {
                case ProjectFileUpdate update -> {
                    try {
                        IOUtils.write(update.getAfter().get(),
                                new FileOutputStream(update.getLocation().getAbsolutePath().toFile()),
                                StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                case ProjectFileCreation creation -> {
                    try {
                        IOUtils.write(creation.getAfter().get(),
                                new FileOutputStream(creation.getLocation().getAbsolutePath().toFile()),
                                StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                case ProjectFileDeletion deletion -> {
                    try {
                        Files.delete(deletion.getLocation().getAbsolutePath());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                default -> throw new IllegalStateException("Not implemented yet: " + operation.getType());
            }
        }
    }

    private Mono<? extends PatchOperationResultDetail> checkForExistingRemoteBranch(
            PatchExecutionContext executionContext) {
        SortedSet<String> remoteBranches = this.gitService.getBranchState(
                executionContext.getWorkingCopy()).getRemoteBranches();
        if (!remoteBranches.contains(executionContext.getPatchBranch())) {
            log.info("Detected existing remote branch \"{}\", cannot patch \"{}\".",
                    executionContext.getPatchBranch(),
                    executionContext.getFQPN());
            return Mono.empty();
        }

        return Mono.fromSupplier(() -> {
            String href = getPatchRemoteBranchHref(executionContext);

            return PatchExecutionResult.RemoteBranchExistsResultDetail.builder()
                    .remoteBranch(RemoteBranch.builder()
                            .name(executionContext.getPatchBranch())
                            .href(href)
                            .build())
                    .build();
        }).doOnSubscribe(_ -> executionContext.publish("Checking for remote branches"));
    }

    private String getPatchRemoteBranchHref(PatchOperationContext executionContext) {
        // TODO branch url for every git provider
        return projectService.require(executionContext)
                .getMetaData()
                .getBrowserLink()
                .map(link -> link + "../../branch/" + executionContext.getPatchBranch())
                .orElse(null);
    }


    private Mono<? extends PatchOperationResultDetail> makeSourceCodeChanges(PatchExecutionContext executionContext) {

        return Mono.fromSupplier(() -> {
                    PatchContextImpl patchContext = new PatchContextImpl(
                            projectService.require(executionContext),
                            workingCopyService.require(executionContext));
                    WorkingCopy workingCopy = executionContext.getWorkingCopy();

                    // switch to target branch
                    gitService.execute(workingCopy, git -> {
                        git.checkout().setName(executionContext.getBaseBranch()).call();
                        git.pull().setCredentialsProvider(workingCopy.getCredentialsProvider())
                                .call();
                    });

                    // execute patch
                    executionContext.getPatch().execute(patchContext);

                    List<ProjectFileOperation> operations = patchContext.getOperations();
                    if (operations.isEmpty()) {
                        return PatchExecutionResult.NoopResultDetail.builder().build();
                    } else {
                        String unifiedDiff = operations
                                .stream()
                                .map(this::toUnifiedDiff)
                                .collect(joining("\n"));
                        return PatchExecutionResult.PreviewGeneratedResultDetail.builder()
                                .operations(operations)
                                .unifiedDiff(unifiedDiff).build();
                    }
                })
                .doOnSubscribe(_ -> executionContext.publish("Evaluating changes"));
    }

    private Mono<PatchOperationResultDetail> checkForExistingPullRequest(PatchExecutionContext context) {
        return getRelevantPullRequest(context)
                .doOnSubscribe(s -> context.publish("Checking for existing PRs"))
                .flatMap(pullRequest ->
                        pullRequest
                                .map(request ->
                                        Mono.just(PatchExecutionResult.PullRequestStillOpenResultDetail.builder()
                                                .pullRequest(request)
                                                .remoteBranch(RemoteBranch.builder()
                                                        .name(context.getPatchBranch())
                                                        .href(this.getPatchRemoteBranchHref(context))
                                                        .build())
                                                .build()))
                                .orElseGet(Mono::empty)
                );
    }

    private Mono<Optional<PullRequest>> getRelevantPullRequest(PatchOperationContext context) {
        return gitService.getOpenPullRequests(context)
                .doOnNext(pullRequests -> {
                    for (PullRequest pR : pullRequests) {
                        log.info("{}: {} -> {}", pR.getTitle(), pR.getSourceBranchName(),
                                pR.getTargetBranchName());
                    }
                })
                .map(pullRequests -> {
                    // try to find matching pull request
                    return pullRequests.stream()
                            .filter(pR -> pR.getTargetBranchName().equals(context.getBaseBranch())
                                    && pR.getSourceBranchName().equals(context.getPatchBranch()))
                            .findAny();
                });
    }

    private Mono<Void> resetWorkspace(PatchExecutionContext executionContext) {
        return Mono.defer(() -> {
            executionContext.publish("Resetting working copy");
            return workingCopyService.reset(executionContext.getWorkingCopy());
        });
    }


    private String getPatchBranch(Patch patch) {
        String sanitizedPatchId = patch.getMetaData().getId().replaceAll("[^a-zA-Z0-9._/-]", "_");
        return "project-maintainer/" + sanitizedPatchId;
    }


    private Patch requirePatch(String id) {
        return patches.stream()
                .filter(p -> p.getMetaData().getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Patch not found"));
    }

    private String toUnifiedDiff(ProjectFileOperation operation) {
        List<String> before =
                operation.getBefore().map(c -> List.of(c.split("\n"))).orElse(emptyList());
        List<String> after =
                operation.getAfter().map(c -> List.of(c.split("\n"))).orElse(emptyList());
        com.github.difflib.patch.Patch<String> patches = DiffUtils.diff(before, after);

        String filePath = operation.getLocation().getRelativePath().toString();
        List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
                operation.getBefore().map(_ -> filePath).orElse(null),
                operation.getAfter().map(_ -> filePath).orElse(null),
                before,
                patches, 2);

        List<String> header = new ArrayList<>();
        header.add("diff --git a/%s b/%s".formatted(filePath, filePath));
        if (operation.getType() == ProjectFileOperationType.ADD) {
            header.add("new file mode 100644");
        } else if (operation.getType() == ProjectFileOperationType.DELETE) {
            header.add("deleted file mode 100644");
        }
        header.add("index %s..%s".formatted(
                operation.getBefore().map(DigestUtils::md5Hex).orElse("000000000000"),
                operation.getAfter().map(DigestUtils::md5Hex).orElse("000000000000")));
        return Stream.concat(header.stream(), unifiedDiff.stream())
                .collect(joining("\n"));
    }

}
