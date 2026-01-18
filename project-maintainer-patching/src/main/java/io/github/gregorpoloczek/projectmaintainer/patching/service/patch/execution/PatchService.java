package io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress.State;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress.ProjectOperationProgressBuilder;
import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.ProjectDiscovery.PullRequestCreation;
import io.github.gregorpoloczek.projectmaintainer.core.domain.discovery.service.PullRequest;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition.Patch;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition.PatchMetaData;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition.ProjectFileCreation;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition.ProjectFileDeletion;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition.ProjectFileOperation;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition.ProjectFileOperationType;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.definition.ProjectFileUpdate;
import io.github.gregorpoloczek.projectmaintainer.patching.service.patch.execution.PatchExecutionResult.PreviewGeneratedResultDetail;
import io.github.gregorpoloczek.projectmaintainer.scm.service.git.BranchState;
import io.github.gregorpoloczek.projectmaintainer.scm.service.git.GitService;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopyService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.SortedSet;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
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
    final List<Patch> patches = new ArrayList<>();
    ProjectService projectService;
    GitService gitService;
    private final ConversionService conversionService;

    public List<PatchMetaData> getAvailablePatches() {
        return this.patches.stream()
                .map(Patch::getMetaData)
                .sorted(Comparator.comparing(PatchMetaData::getId))
                .toList();
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
            String defaultBranch = this.gitService.execute(workingCopy, c -> {
                return c.getBranchState().getDefaultBranch();
            });
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
            SortedSet<String> remoteBranches = this.gitService.execute(stopContext.getWorkingCopy(), c -> {
                return c.getBranchState().getRemoteBranches();
            });
            if (remoteBranches.contains(stopContext.getPatchBranch())) {
                log.info("Deleting branch \"{}\" from \"{}\".", stopContext.getPatchBranch(), stopContext.getFQPN());
                RemoteBranch branch =
                        RemoteBranch.builder()
                                .name(stopContext.getPatchBranch())
                                .href(getPatchRemoteBranchHref(stopContext))
                                .build();
                gitService.execute(stopContext.getWorkingCopy(), gitActionContext -> {
                    // TODO where is the delete operation?
                    // delete origin branch
                    gitActionContext.command(Git::push)
                            .setRemote("origin")
                            .setRefSpecs(new RefSpec(":refs/heads/" + stopContext.getPatchBranch()))
                            .call();

                    // delete local branch by pruning
                    gitActionContext.command(Git::fetch)
                            .setRemoveDeletedRefs(true)
                            .call();
                });

                return PatchStopResult.DoneResultDetail.builder()
                        .remoteBranch(branch)
                        .pullRequest(pullRequest.orElse(null))
                        .build();
            } else {
                return PatchStopResult.NoopResultDetail.builder().build();
            }
        }).doOnSubscribe(x -> stopContext.publish("Deleting patch branch"));
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
                        // TODO [Patching] add pull request to result
                        return this.gitService.closePullRequest(stopContext, pullRequest.get())
                                .then(Mono.just(pullRequest));
                    }
                })
                .doOnSubscribe(x -> stopContext.publish("Close pull request"));
    }

    private Flux<ProjectOperationProgress<PatchExecutionResult>> usePatch(ProjectRelatable projectRelatable,
                                                                          String id, boolean previewOnly) {
        Patch patch = this.requirePatch(id);
        FQPN fqpn = projectRelatable.getFQPN();
        WorkingCopy workingCopy = this.workingCopyService.require(projectRelatable);

        return Flux.<ProjectOperationProgress<PatchExecutionResult>>create(sink -> {
                    ProjectOperationProgressBuilder<PatchExecutionResult> progress =
                            ProjectOperationProgress.<PatchExecutionResult>builder().fqpn(fqpn);

                    ProgressSink<PatchExecutionResult> progressSink = new ProgressSink<>(sink, progress);

                    sink.next(progress.state(State.SCHEDULED).build());
                    progressSink.total(previewOnly ? 4 : 6);
                    progress.state(State.RUNNING);

                    String defaultBranch = this.gitService.execute(workingCopy, c -> {
                        return c.getBranchState().getDefaultBranch();
                    });
                    PatchExecutionContext executionContext = PatchExecutionContext.builder()
                            .workingCopy(workingCopy)
                            .progressSink(progressSink)
                            .patch(patch)
                            .defaultBranch(defaultBranch)
                            .baseBranch(defaultBranch)
                            .patchBranch(this.getPatchBranch(patch))
                            .build();

                    // TODO [Patching] lock working copy

                    log.info("A");
                    // TODO [Patching] umbauen auf ohne "switchIfEmpty"
                    Mono.empty()
                            .then(this.resetWorkingCopy(executionContext))
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
                }).map(p -> {
                    if (p.getState() == State.DONE) {
                        String message = p.getResult()
                                .map(PatchExecutionResult::getDetail)
                                .map(PatchOperationResultDetail::getName)
                                .orElse(p.getMessage());
                        return p.toBuilder().message(message).build();
                    }
                    return p;
                })
                .onErrorResume(t -> Flux.just(ProjectOperationProgress.<PatchExecutionResult>builder()
                        .fqpn(projectRelatable.getFQPN())
                        .state(State.FAILED)
                        .throwable(t)
                        .build()).concatWith(Mono.error(t)));
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
                .doOnSubscribe(x -> executionContext.publish("Creating pull request"));
    }

    private String getPullRequestTitle(Patch patch) {
        // TODO [Patching] better title
        return patch.getMetaData().getDescription();
    }

    private Mono<RemoteBranch> createBranchWithChanges(
            PatchExecutionContext executionContext,
            PreviewGeneratedResultDetail detail
    ) {
        return Mono.fromSupplier(() -> {

            gitService.execute(executionContext.getWorkingCopy(), gitActionContext -> {
                BranchState branchState = gitActionContext.getBranchState();
                gitActionContext.command(Git::checkout)
                        .setName(branchState.getDefaultBranch()).call();

                try {
                    // delete previously created local branch (in case it was retained)
                    if (branchState.getLocalBranches().contains(executionContext.getPatchBranch())) {
                        log.warn("Detected obsolete local branch {} in {}, deleting it.",
                                executionContext.getPatchBranch(),
                                executionContext.getFQPN());
                        // delete local branch
                        gitActionContext.command(Git::branchDelete)
                                .setForce(true)
                                .setBranchNames(executionContext.getPatchBranch())
                                .call();
                    }

                    // create local branch
                    gitActionContext
                            .command(Git::checkout)
                            .setCreateBranch(true)
                            .setName(executionContext.getPatchBranch()).call();

                    // push branch to remote
                    gitActionContext.command(Git::push)
                            .setRemote("origin")
                            .add(executionContext.getPatchBranch())
                            .call();

                    // apply changes in file system
                    this.applyOperationsToFileSystem(detail.getOperations());

                    // stage changes
                    for (ProjectFileOperation o : detail.getOperations()) {
                        String pattern = o.getLocation().getRelativePath().toString();
                        if (o instanceof ProjectFileDeletion) {
                            gitActionContext.command(Git::rm).addFilepattern(pattern).call();
                        } else {
                            gitActionContext.command(Git::add).addFilepattern(pattern).call();
                        }
                    }

                    // commit changes
                    gitActionContext.command(Git::commit).setMessage(getCommitMessage(executionContext.getPatch())).call();

                    // push changes
                    gitActionContext.command(Git::push).call();

                    gitActionContext.command(Git::checkout).setName(branchState.getDefaultBranch()).call();
                    gitActionContext.command(Git::branchDelete).setBranchNames(executionContext.getPatchBranch()).setForce(true).call();
                } finally {
                    workingCopyService.reset(executionContext.getWorkingCopy());
                }
            });
            return RemoteBranch.builder()
                    .name(executionContext.getPatchBranch())
                    .href(getPatchRemoteBranchHref(executionContext))
                    .build();
        }).doOnSubscribe(x -> executionContext.publish("Applying changes"));
    }

    private String getCommitMessage(Patch patch) {
        PatchMetaData metaData = patch.getMetaData();
        String prefix = metaData.getCommitPrefix().map(p -> p + " ").orElse("");
        String defaultCommitMessage = prefix + metaData.getDescription();

        return metaData.getCommitMessage().orElse(defaultCommitMessage);
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
        SortedSet<String> remoteBranches = this.gitService.execute(
                executionContext.getWorkingCopy(), c -> {
                    return c.getBranchState().getRemoteBranches();
                });
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
        }).doOnSubscribe(x -> executionContext.publish("Checking for remote branches"));
    }

    private String getPatchRemoteBranchHref(PatchOperationContext executionContext) {
        // TODO [Patching] branch url for every git provider
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
                    gitService.execute(workingCopy, gitActionContext -> {
                        gitActionContext.command(Git::checkout).setName(executionContext.getBaseBranch()).call();
                        gitActionContext.command(Git::pull).call();
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
                .doOnSubscribe(x -> executionContext.publish("Evaluating changes"));
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

    private Mono<Void> resetWorkingCopy(PatchExecutionContext executionContext) {
        return Mono.fromRunnable(() -> {
            executionContext.publish("Resetting working copy");

            workingCopyService.reset(executionContext.getWorkingCopy());
        });
    }


    private String getPatchBranch(Patch patch) {
        String sanitizedPatchId = patch.getMetaData().getId().replaceAll("[^a-zA-Z0-9._/-]", "_");
        String defaultBranchName = "project-maintainer/" + sanitizedPatchId;
        return patch.getMetaData().getBranchName().orElse(defaultBranchName);
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
                operation.getBefore().map(x -> filePath).orElse(null),
                operation.getAfter().map(x -> filePath).orElse(null),
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

    @PostConstruct
    void init() {
        ServiceLoader.load(Patch.class).stream()
                .map(ServiceLoader.Provider::get)
                .forEach(this.patches::add);
    }

}
