package de.gregorpoloczek.projectmaintainer.core.domain.git.service;

import de.gregorpoloczek.projectmaintainer.core.common.properties.ApplicationProperties;
import de.gregorpoloczek.projectmaintainer.core.domain.project.repository.ProjectImpl;
import de.gregorpoloczek.projectmaintainer.core.domain.project.repository.ProjectRepository;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectNotFoundException;
import de.gregorpoloczek.projectmaintainer.core.domain.project.service.common.FQPN;
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
    private Map<FQPN, WorkingCopyImpl> workingCopies = new TreeMap<>();
    private final ProjectRepository projectRepository;

    public WorkingCopyService(final ApplicationProperties applicationProperties,
            final ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
        this.projectsDirectory = applicationProperties.getProjects().getCloneDirectory();
    }

    public WorkingCopyImpl save(FQPN fqpn, URI uri, File directory, Commit latestCommit,
            CredentialsProvider credentialsProvider) {
        final ProjectImpl project = projectRepository.find(fqpn)
                .orElseThrow(() -> new ProjectNotFoundException(fqpn));

        project.markAsCloned();
        project.setLatestCommit(latestCommit);

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
