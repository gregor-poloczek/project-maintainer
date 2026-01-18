package io.github.gregorpoloczek.projectmaintainer.analysis.service.fulltext.analyzers.common;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.ProjectFileLocationImpl;
import io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy.WorkingCopy;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.mutable.MutableBoolean;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectFilesImpl implements ProjectFiles {

    WorkingCopy workingCopy;

    // TODO bestimmte Verzeichnisse black listen, z.B: ".git"

    @Override
    public boolean hasAny(final String regex) {
        MutableBoolean result = new MutableBoolean(false);
        try {
            Files.walkFileTree(workingCopy.getDirectory().toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
                        throws IOException {
                    if (result.booleanValue()) {
                        return FileVisitResult.TERMINATE;
                    }
                    return super.preVisitDirectory(dir, attrs);
                }

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                    if (Pattern.compile(regex).matcher(file.toFile().getAbsolutePath()).find()) {
                        result.setTrue();
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result.booleanValue();
    }

    public SortedSet<File> find(String regex) {
        SortedSet<File> result = new TreeSet<>();
        try {
            Files.walkFileTree(workingCopy.getDirectory().toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
                        throws IOException {
                    return super.preVisitDirectory(dir, attrs);
                }

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                    if (Pattern.compile(regex).matcher(file.toFile().getName()).find()) {
                        result.add(file.toFile());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    @Override
    public List<ProjectFileLocation> findLocations(String regex) {
        return this.find(regex).stream()
                .map(f -> ProjectFileLocationImpl.of(workingCopy, f))
                .map(ProjectFileLocation.class::cast)
                .toList();
    }

    @Override
    public Optional<ProjectFileLocation> findLocation(String regex) {
        return this.find(regex).stream()
                .map(f -> ProjectFileLocationImpl.of(workingCopy, f))
                .map(ProjectFileLocation.class::cast)
                .findFirst();
    }

    @Override
    public ProjectFileLocation get(String path) {
        return workingCopy.createLocation(path);
    }

}
