package de.gregorpoloczek.projectmaintainer.core.domain.git.service;

import de.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.ProjectNotClonedException;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectOperationProgressListener;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectService;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
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
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WorkingCopyService {

    private final File projectsDirectory;
    private final GitService gitService;
    private final ProjectService projectService;
    private final Map<FQPN, WorkingCopyImpl> workingCopies = new TreeMap<>();

    public WorkingCopyService(
            final ApplicationProperties applicationProperties,
            final GitService gitService,
            final ProjectService projectService
    ) {
        this.projectsDirectory = applicationProperties.getProjects().getCloneDirectory();
        this.gitService = gitService;
        this.projectService = projectService;
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
                result.getLatestCommit().orElse(null),
                workingCopy.getCredentialsProvider()
        );
    }

    public void wipeProject(@NonNull final FQPN fqpn,
            @NonNull final ProjectOperationProgressListener listener) {
        // TODO move code to working copy service
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


    public WorkingCopyImpl save(FQPN fqpn, URI uri, File directory, Commit latestCommit,
            CredentialsProvider credentialsProvider) {
        final WorkingCopyImpl result = new WorkingCopyImpl(fqpn, uri, directory, latestCommit,
                credentialsProvider);
        this.workingCopies.put(fqpn, result);
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

        this.workingCopies.remove(fqpn);
        log.info("Removed working copy of \"{}\".", fqpn);
    }

    public Optional<WorkingCopy> find(@NonNull FQPN fqpn) {
        return Optional.ofNullable(this.workingCopies.get(fqpn));
    }

    public List<WorkingCopy> findAll() {
        return new ArrayList<>(this.workingCopies.values());
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

                this.workingCopies.put(fqpn, new WorkingCopyImpl(fqpn, URI.create(url), file, null, null));
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

        return new WorkingCopyImpl(fqpn, uri, directory, null, credentialsProvider);
    }
}
