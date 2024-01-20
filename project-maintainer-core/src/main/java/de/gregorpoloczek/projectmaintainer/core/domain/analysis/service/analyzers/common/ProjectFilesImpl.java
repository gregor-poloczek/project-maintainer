package de.gregorpoloczek.projectmaintainer.core.domain.analysis.service.analyzers.common;

import de.gregorpoloczek.projectmaintainer.core.domain.project.service.dtos.Project;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class ProjectFilesImpl implements ProjectFiles {

  private final Project project;

  public ProjectFilesImpl(final Project project) {
    this.project = project;
  }

  @Override
  public boolean hasAny(final String regex) {
    MutableBoolean result = new MutableBoolean(false);
    try {
      Files.walkFileTree(project.getDirectory().toPath(), new SimpleFileVisitor<>() {
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
          if (Pattern.compile(regex).matcher(file.toFile().getName()).find()) {
            result.setTrue();
            return FileVisitResult.TERMINATE;
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return result.booleanValue();
  }

  public SortedSet<File> find(String regex) {
    SortedSet<File> result = new TreeSet<>();
    try {
      Files.walkFileTree(project.getDirectory().toPath(), new SimpleFileVisitor<>() {
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
      throw new RuntimeException(e);
    }
    return result;
  }

}
