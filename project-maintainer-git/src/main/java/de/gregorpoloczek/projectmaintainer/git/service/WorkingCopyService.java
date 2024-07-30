package de.gregorpoloczek.projectmaintainer.git.service;

import de.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import de.gregorpoloczek.projectmaintainer.core.common.repository.GenericProjectRelatableRepository;
import de.gregorpoloczek.projectmaintainer.core.common.repository.ProjectRelatableRepository;
import de.gregorpoloczek.projectmaintainer.core.domain.communication.service.ProjectOperationProgressListener;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.Project;
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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WorkingCopyService {

    private final File projectsDirectory;
    private final GitService gitService;
    private final ProjectService projectService;
    private final WorkingCopyRepository workingCopyRepository;

    public WorkingCopyService(
            final ApplicationProperties applicationProperties,
            final GitService gitService,
            final ProjectService projectService,
            WorkingCopyRepository workingCopyRepository
    ) {
        this.projectsDirectory = applicationProperties.getProjects().getCloneDirectory();
        this.gitService = gitService;
        this.projectService = projectService;
        this.workingCopyRepository = workingCopyRepository;
    }

    public void cloneProject(@NonNull FQPN fqpn, @NonNull ProjectOperationProgressListener listener) {
        final Project project = this.projectService.requireProject(fqpn);
        final WorkingCopy workingCopy =
                this.createNew(project.getMetaData().getFQPN(), project.getURI(),
                        project.getCredentialsProvider());
        final CloneResult clone = this.gitService.clone(workingCopy, listener);
        this.save(
                workingCopy.getFQPN(),
                workingCopy.getURI(),
                workingCopy.getDirectory(),
                clone.getCurrentBranch(),
                clone.getLatestCommit().orElse(null),
                workingCopy.getCredentialsProvider()
        );
    }

    public void pullProject(@NonNull FQPN fqpn, @NonNull ProjectOperationProgressListener listener) {
        final WorkingCopy workingCopy = this.find(fqpn)
                .orElseThrow(() -> new ProjectNotClonedException(fqpn));
        final PullResult result = this.gitService.pull(workingCopy, listener);
        this.save(
                workingCopy.getFQPN(),
                workingCopy.getURI(),
                workingCopy.getDirectory(),
                workingCopy.getCurrentBranch(),
                result.getLatestCommit().orElse(null),
                workingCopy.getCredentialsProvider()
        );
    }

    public void wipeProject(@NonNull final FQPN fqpn,
            @NonNull final ProjectOperationProgressListener listener) {
        final Project project = this.projectService.requireProject(fqpn);
        project.withWriteLock(() -> {
            try {
                this.remove(fqpn);
                listener.succeeded(project);
            } catch (Exception e) {
                listener.failed(project, e);
            }
            return null;
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
        if (!workingCopy.isPresent()) {
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
                        .latestCommit(revCommits.stream().findFirst().map(CommitImpl::of).orElse(null))
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
        File directory = this.projectsDirectory.toPath().resolve(
                Path.of(fqpn.getValue().replaceAll("::", "/"))
        ).toFile();

        return WorkingCopyImpl.builder()
                .fqpn(fqpn)
                .directory(directory)
                .uri(uri)
                .credentialsProvider(credentialsProvider)
                .build();

    }
}
