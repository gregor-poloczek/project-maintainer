package io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectRelatable;
import io.github.gregorpoloczek.projectmaintainer.scm.service.git.Commit;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

public interface WorkingCopy extends ProjectRelatable {

    FQPN getFQPN();

    File getDirectory();

    Optional<Commit> getLatestCommit();

    String getCurrentBranch();

    URI getURI();

    <T> T withReadLock(Supplier<T> operation);

    void withReadLock(Runnable operation);

    <T> T withWriteLock(Supplier<T> operation);

    <T> T withWriteLockAndThrowing(final Operation<T> operation) throws Exception;

    @FunctionalInterface
    interface Operation<T> {

        T execute() throws Exception;
    }

    void withWriteLock(Runnable operation);

    ProjectFileLocation createLocation(Path path);

    default ProjectFileLocation createLocation(String path) {
        return createLocation(Path.of(path));
    }

    void writeLock();

    void writeUnlock();
}
