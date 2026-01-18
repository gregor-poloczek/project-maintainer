package io.github.gregorpoloczek.projectmaintainer.scm.service.workingcopy;

import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.FQPN;
import io.github.gregorpoloczek.projectmaintainer.core.domain.project.service.ProjectFileLocation;
import io.github.gregorpoloczek.projectmaintainer.scm.service.git.Commit;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Builder
@Slf4j
public class WorkingCopyImpl implements WorkingCopy {

    private final String currentBranch;
    private final FQPN fqpn;
    private final File directory;
    private final URI uri;
    private final Commit latestCommit;
    @Getter(AccessLevel.NONE)
    @Builder.Default
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    public Optional<Commit> getLatestCommit() {
        return Optional.ofNullable(latestCommit);
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public FQPN getFQPN() {
        return fqpn;
    }


    @Override
    public <T> T withReadLock(final Supplier<T> operation) {
        lock.readLock().lock();
        try {
            return operation.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T> T withWriteLock(final Supplier<T> operation) {
        lock.writeLock().lock();
        try {
            return operation.get();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public <T> T withWriteLockAndThrowing(final Operation<T> operation) throws Exception {
        lock.writeLock().lock();
        try {
            return operation.execute();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void withReadLock(final Runnable operation) {
        this.withReadLock(((Supplier<Void>) (() -> {
            operation.run();
            return null;
        })));
    }

    @Override
    public void withWriteLock(final Runnable operation) {
        this.withWriteLock(((Supplier<Void>) (() -> {
            operation.run();
            return null;
        })));
    }

    @Override
    public ProjectFileLocation createLocation(Path path) {
        return ProjectFileLocationImpl.of(this,
                this.getDirectory().toPath().resolve(path).toFile());
    }

    @Override
    public void writeLock() {
        log.info("Write locking working copy of {}", this.getFQPN());
        //this.lock.writeLock().lock();
    }

    @Override
    public void writeUnlock() {
        log.info("Write unlocking working copy of {}", this.getFQPN());
        //this.lock.writeLock().unlock();
    }
}
