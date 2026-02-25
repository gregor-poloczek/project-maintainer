package io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy;

import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress.State;
import io.github.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.events.ProjectCreatedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.events.ProjectDeletedEvent;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.Workspace;
import io.github.gregorpoloczek.projectmaintainer.core.domain.workspace.service.facets.BelongsToWorkspace;
import io.github.gregorpoloczek.projectmaintainer.scm.service.git.CloneResult;
import io.github.gregorpoloczek.projectmaintainer.scm.service.git.Commit;
import io.github.gregorpoloczek.projectmaintainer.scm.service.git.GitService;
import io.github.gregorpoloczek.projectmaintainer.scm.service.git.PullResult;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WorkingCopyService {

    GitService gitService;
    ProjectService projectService;
    WorkingCopyRepository workingCopyRepository;

    public Flux<ProjectOperationProgress<Void>> cloneProject(@NonNull ProjectRelatable projectRelatable) {
        final Project project = this.projectService.require(projectRelatable);
        final WorkingCopy workingCopy =
                this.createNew(project.getMetaData().getFQPN(), project.getURI());
        final Flux<ProjectOperationProgress<CloneResult>> clone = this.gitService.clone(workingCopy);

        return clone.doOnNext(p -> {
                    if (p.getState() == OperationProgress.State.DONE) {
                        CloneResult result = p.getResult().orElseThrow();
                        this.save(
                                workingCopy.getFQPN(),
                                workingCopy.getURI(),
                                workingCopy.getDirectory(),
                                result.getCurrentBranch(),
                                result.getLatestCommit().orElse(null)
                        );
                    }
                })
                .map(this::toProgressWithoutResult);

    }

    public Flux<ProjectOperationProgress<Void>> pullProject(@NonNull ProjectRelatable projectRelatable) {
        final WorkingCopy workingCopy = this.require(projectRelatable);
        final Flux<ProjectOperationProgress<PullResult>> pull = this.gitService.pull(workingCopy);

        return pull.doOnNext(p -> {
            if (p.getState() == OperationProgress.State.DONE) {
                PullResult result = p.getResult().orElseThrow();
                this.save(
                        workingCopy.getFQPN(),
                        workingCopy.getURI(),
                        workingCopy.getDirectory(),
                        workingCopy.getCurrentBranch(),
                        result.getLatestCommit().orElse(null)
                );
            }
        }).map(this::toProgressWithoutResult);
    }

    private ProjectOperationProgress<Void> toProgressWithoutResult(ProjectOperationProgress<?> p) {
        return ProjectOperationProgress.<Void>builder()
                .fqpn(p.getFQPN())
                .state(p.getState())
                .throwable(p.getThrowable().orElse(null))
                .progressCurrent(p.getProgressCurrent())
                .progressTotal(p.getProgressTotal())
                .message(p.getMessage())
                .build();
    }

    public Flux<ProjectOperationProgress<Void>> wipeProject(@NonNull final ProjectRelatable projectRelatable) {
        final Project project = this.projectService.require(projectRelatable);
        return Flux.create(sink -> {
            sink.next(ProjectOperationProgress.<Void>builder()
                    .fqpn(project.getFQPN())
                    .message("Removing working copy")
                    .state(State.SCHEDULED)
                    .build());
            project.withWriteLock(() -> {
                try {
                    this.remove(projectRelatable.getFQPN());
                    sink.next(ProjectOperationProgress.<Void>builder()
                            .fqpn(project.getFQPN())
                            .message("Working copy removed")
                            .state(OperationProgress.State.DONE)
                            .progressCurrent(1)
                            .progressTotal(1)
                            .result(null)
                            .build());
                    sink.complete();
                } catch (Exception e) {
                    sink.next(ProjectOperationProgress.<Void>builder()
                            .fqpn(project.getFQPN())
                            .throwable(e)
                            .state(OperationProgress.State.FAILED)
                            .build());
                    sink.error(e);
                }
                return null;
            });
        });
    }


    public WorkingCopyImpl save(FQPN fqpn, URI uri, File directory, String currentBranch, Commit latestCommit) {
        final WorkingCopyImpl result =
                WorkingCopyImpl.builder()
                        .fqpn(fqpn)
                        .uri(uri)
                        .directory(directory)
                        .currentBranch(currentBranch)
                        .latestCommit(latestCommit)
                        .build();
        this.workingCopyRepository.save(fqpn, result);
        return result;
    }

    public void remove(FQPN fqpn) {
        final Optional<WorkingCopy> workingCopy = this.find(fqpn);
        if (workingCopy.isEmpty()) {
            log.info("Project \"{}\" does not have a working copy, nothing to remove.", fqpn);
            return;
        }

        Path workingCopyPath = workingCopy.get().getDirectory().toPath();

        final File directory = workingCopy.get().getDirectory();
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        this.workingCopyRepository.delete(fqpn);
        log.info("Deleted working copy of \"{}\" at {}.", fqpn, workingCopyPath);
    }

    public Optional<WorkingCopy> find(@NonNull ProjectRelatable identifiesProject) {
        return workingCopyRepository.find(identifiesProject);
    }

    public boolean hasWorkspace(@NonNull ProjectRelatable identifiesProject) {
        return this.find(identifiesProject).isPresent();
    }

    public WorkingCopy require(@NonNull ProjectRelatable identifiesProject) {
        return workingCopyRepository.require(identifiesProject);
    }

    public List<WorkingCopy> findAll() {
        return workingCopyRepository.findAll();
    }

    @EventListener
    void on(ProjectDeletedEvent event) {
        this.remove(event.getId());
    }

    @EventListener
    void on(ProjectCreatedEvent event) {
        Path workingCopyDirectoryPath = this.calculateWorkingCopyDirectory(event);
        Path gitDirectoryPath = workingCopyDirectoryPath.resolve(".git");
        File gitDirectory = gitDirectoryPath.toFile();
        if (!gitDirectory.exists() || !gitDirectory.isDirectory()) {
            return;
        }

        log.info("Found already existing working copy for project {} at {}, reusing it.", event.getFQPN(), workingCopyDirectoryPath);

        this.restoreExistingWorkingCopy(event, workingCopyDirectoryPath);
    }

    private void restoreExistingWorkingCopy(ProjectRelatable projectRelatable, Path workingCopyDirectoryPath) {
        try (Git git = Git.open(workingCopyDirectoryPath.toFile())) {
            String url = git.getRepository().getConfig().getString("remote", "origin", "url");
            List<RevCommit> revCommits = new ArrayList<>();
            try {
                git.log().setMaxCount(1).call().forEach(revCommits::add);
            } catch (NoHeadException e) {
                log.warn("No header found for {}, repository possibly completely empty.", projectRelatable.getFQPN());
            }
            String currentBranch = git.getRepository().getBranch();

            WorkingCopyImpl workingCopy = WorkingCopyImpl.builder()
                    .fqpn(projectRelatable.getFQPN())
                    .uri(URI.create(url))
                    .directory(workingCopyDirectoryPath.toFile())
                    .currentBranch(currentBranch)
                    .latestCommit(revCommits.stream().findFirst().map(Commit::of).orElse(null))
                    .build();
            this.workingCopyRepository.save(projectRelatable, workingCopy);
        } catch (GitAPIException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    public WorkingCopy createNew(final FQPN fqpn, final URI uri) {
        File directory = calculateWorkingCopyDirectory(fqpn).toFile();

        return WorkingCopyImpl.builder()
                .fqpn(fqpn)
                .directory(directory)
                .uri(uri)
                .build();
    }

    private @NonNull Path calculateWorkingCopyDirectory(ProjectRelatable projectRelatable) {
        Workspace workspace = projectService.require(projectRelatable).requireFacet(BelongsToWorkspace.class).getWorkspace();
        return workspace.getDirectory()
                .resolve("working-copies")
                .resolve(Path.of(projectRelatable.getFQPN().toString().replaceAll("::", "/")));
    }

    public void reset(WorkingCopy workingCopy) {
        this.gitService.execute(workingCopy, (gitActionContext) -> {
            // remove all changes
            gitActionContext.command(Git::reset)
                    .setMode(ResetType.HARD)
                    .call();
            gitActionContext.command(Git::clean)
                    .setCleanDirectories(true)
                    .call();
            // check out default branch
            gitActionContext.command(Git::checkout)
                    .setName(gitActionContext.getBranchState().getDefaultBranch())
                    .call();
        });
    }

}
