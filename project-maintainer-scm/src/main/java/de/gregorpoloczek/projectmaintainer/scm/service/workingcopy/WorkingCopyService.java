package de.gregorpoloczek.projectmaintainer.scm.service.workingcopy;

import de.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.OperationProgress.State;
import de.gregorpoloczek.projectmaintainer.core.common.service.progress.ProjectOperationProgress;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
import de.gregorpoloczek.projectmaintainer.scm.service.git.BranchState;
import de.gregorpoloczek.projectmaintainer.scm.service.git.CloneResult;
import de.gregorpoloczek.projectmaintainer.scm.service.git.Commit;
import de.gregorpoloczek.projectmaintainer.scm.service.git.GitService;
import de.gregorpoloczek.projectmaintainer.scm.service.git.PullResult;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WorkingCopyService {

    ApplicationProperties applicationProperties;
    GitService gitService;
    ProjectService projectService;
    WorkingCopyRepository workingCopyRepository;

    public Flux<ProjectOperationProgress<Void>> cloneProject(@NonNull ProjectRelatable projectRelatable) {
        final Project project = this.projectService.require(projectRelatable);
        final WorkingCopy workingCopy =
                this.createNew(project.getMetaData().getFQPN(), project.getURI(),
                        project.getCredentialsProvider());
        final Flux<ProjectOperationProgress<CloneResult>> clone = this.gitService.clone(workingCopy);

        return clone.doOnNext(p -> {
                    if (p.getState() == OperationProgress.State.DONE) {
                        CloneResult result = p.getResult();
                        this.save(
                                workingCopy.getFQPN(),
                                workingCopy.getURI(),
                                workingCopy.getDirectory(),
                                result.getCurrentBranch(),
                                result.getLatestCommit().orElse(null),
                                workingCopy.getCredentialsProvider()
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
                PullResult result = p.getResult();
                this.save(
                        workingCopy.getFQPN(),
                        workingCopy.getURI(),
                        workingCopy.getDirectory(),
                        workingCopy.getCurrentBranch(),
                        result.getLatestCommit().orElse(null),
                        workingCopy.getCredentialsProvider()
                );
            }
        }).map(this::toProgressWithoutResult);
    }

    private ProjectOperationProgress<Void> toProgressWithoutResult(ProjectOperationProgress<?> p) {
        return ProjectOperationProgress.<Void>builder()
                .fqpn(p.getFQPN())
                .state(p.getState())
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
                            .state(OperationProgress.State.FAILED)
                            .build());
                    sink.error(e);
                }
                return null;
            });
        });
    }


    public WorkingCopyImpl save(FQPN fqpn, URI uri, File directory, String currentBranch, Commit latestCommit,
            CredentialsProvider credentialsProvider) {
        final WorkingCopyImpl result =
                WorkingCopyImpl.builder()
                        .fqpn(fqpn)
                        .uri(uri)
                        .directory(directory)
                        .currentBranch(currentBranch)
                        .latestCommit(latestCommit)
                        .credentialsProvider(credentialsProvider)
                        .build();
        this.workingCopyRepository.save(fqpn, result);
        return result;
    }

    public void remove(FQPN fqpn) {
        final Optional<WorkingCopy> workingCopy = this.find(fqpn);
        if (workingCopy.isEmpty()) {
            log.info("Project \"%s\" does not have a working copy, nothing to remove.".formatted(fqpn));
            return;
        }

        final File directory = workingCopy.get().getDirectory();
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        this.workingCopyRepository.delete(fqpn);
        log.info("Removed working copy of \"{}\".", fqpn);
    }

    public Optional<WorkingCopy> find(@NonNull ProjectRelatable identifiesProject) {
        return workingCopyRepository.find(identifiesProject);
    }

    public WorkingCopy require(@NonNull ProjectRelatable identifiesProject) {
        return workingCopyRepository.require(identifiesProject);
    }

    public List<WorkingCopy> findAll() {
        return workingCopyRepository.findAll();
    }

    @PostConstruct
    public void init() {
        File projectsDirectory = getProjectsDirectory();
        if (!projectsDirectory.exists()) {
            try {
                Files.createDirectories(projectsDirectory.toPath());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        final SortedSet<File> files = this.findExistingWorkingCopies();
        for (File file : files) {
            final FQPN fqpn = FQPN.of(
                    projectsDirectory.toPath().relativize(file.toPath()).toString().replaceAll("/", "::"));

            try (Git git = Git.open(file)) {
                String url = git.getRepository().getConfig().getString("remote", "origin", "url");
                List<RevCommit> revCommits = new ArrayList<>();
                git.log().setMaxCount(1).call().forEach(revCommits::add);
                String currentBranch = git.getRepository().getBranch();

                WorkingCopyImpl workingCopy = WorkingCopyImpl.builder()
                        .fqpn(fqpn)
                        .uri(URI.create(url))
                        .directory(file)
                        .currentBranch(currentBranch)
                        .latestCommit(revCommits.stream().findFirst().map(Commit::of).orElse(null))
                        .build();
                this.workingCopyRepository.save(fqpn, workingCopy);
            } catch (GitAPIException e) {
                throw new IllegalStateException(e);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private SortedSet<File> findExistingWorkingCopies() {
        SortedSet<File> existingWorkingCopies = new TreeSet<>();

        try {
            File projectsDirectory = getProjectsDirectory();
            Files.walkFileTree(projectsDirectory.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
                        throws IOException {
                    if (Files.exists(dir.resolve(".git"))) {
                        existingWorkingCopies.add(dir.toFile());
                    }
                    return super.preVisitDirectory(dir, attrs);
                }

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return existingWorkingCopies;
    }


    public WorkingCopy createNew(final FQPN fqpn, final URI uri, CredentialsProvider credentialsProvider) {
        File directory = getProjectsDirectory().toPath().resolve(
                Path.of(fqpn.getValue().replaceAll("::", "/"))
        ).toFile();

        return WorkingCopyImpl.builder()
                .fqpn(fqpn)
                .directory(directory)
                .uri(uri)
                .credentialsProvider(credentialsProvider)
                .build();

    }

    private File getProjectsDirectory() {
        return applicationProperties.getProjects().getCloneDirectory();
    }

    public Mono<Void> reset(WorkingCopy workingCopy) {
        return Mono.fromRunnable(() -> {
            this.gitService.execute(workingCopy, (git) -> {
                git.reset().setMode(ResetType.HARD).call();
                BranchState branchState = this.gitService.getBranchState(workingCopy, git);
                git.checkout().setName(branchState.getDefaultBranch()).call();
            });
        });
    }
}
